package daemon

import (
	"beam-share-cli/internal/config"
	"beam-share-cli/internal/crypto"
	utilsServices "beam-share-cli/utils/services"
	"crypto/aes"
	"crypto/cipher"
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"io"
	"math/rand"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"
)

const socketPath = "/tmp/beamshare.sock"

type MessageIPC struct {
	Event  string `json:"event"`
	ID     string `json:"id"`
	File   string `json:"file"`
	Size   int64  `json:"size"`
	From   string `json:"from"`
	Value  string `json:"value"`
	NodeId string `json:"node_id"`
}

type PendingTransfer struct {
	ID       string      `json:"id"`
	FileName string      `json:"file_name"`
	Size     int64       `json:"size"`
	Sender   string      `json:"sender"`
	SenderID string      `json:"sender_id"`
	Status   string      `json:"status"`
	TCPConn  net.Conn    `json:"-"`
	Response chan string `json:"-"`
}

type CachedDevice struct {
	Info     utilsServices.DeviceInfo
	LastSeen time.Time
}

type DaemonServer struct {
	mu        sync.RWMutex
	devices   map[string]CachedDevice
	transfers map[string]*PendingTransfer
	listener  net.Listener
}

func StartServer() {
	srv := &DaemonServer{
		devices:   make(map[string]CachedDevice),
		transfers: make(map[string]*PendingTransfer),
	}

	portChan := make(chan int)
	go srv.listenTCP(portChan)

	tcpPort := <-portChan

	go srv.registerService(tcpPort)

	go srv.loopCacheRefresh()

	srv.listenIPC()
}

func (s *DaemonServer) listenTCP(portChan chan int) {
	listener, err := net.Listen("tcp", ":0")
	if err != nil {
		panic(err)
	}
	defer listener.Close()
	portChan <- listener.Addr().(*net.TCPAddr).Port

	for {
		conn, err := listener.Accept()
		if err != nil {
			continue
		}
		go s.handleIncomingTCP(conn)
	}
}

func (s *DaemonServer) handleIncomingTCP(conn net.Conn) {
	bl := config.LoadBlackList()
	cfg := config.Load()
	trustedList := config.LoadTrustedDevices()
	decoder := json.NewDecoder(conn)

	var meta struct {
		Name       string `json:"name"`
		Size       int64  `json:"size"`
		SenderName string `json:"sender_name"`
		SenderID   string `json:"sender_id"`
	}

	if err := decoder.Decode(&meta); err != nil {
		conn.Close()
		return
	}

	_, _ = conn.Write([]byte("challenge_test_authentification\n"))

	var signatureBuf = make([]byte, 1024)
	n, err := conn.Read(signatureBuf)
	if err != nil {
		conn.Close()
		return
	}
	_ = string(signatureBuf[:n])

	rand.Seed(time.Now().UnixNano())
	txID := fmt.Sprintf("tx-%03d", rand.Intn(1000))

	if !config.IsTrusted(trustedList, meta.SenderID) && cfg.Visibility == "trusted" {
		s.refuseConnection(conn, txID)
		return
	}

	if config.DeviceInBlackList(bl, meta.SenderID) {
		s.refuseConnection(conn, txID)
		return
	}

	transfer := &PendingTransfer{
		ID:       txID,
		FileName: meta.Name,
		Size:     meta.Size,
		Sender:   meta.SenderName,
		SenderID: meta.SenderID,
		Status:   "pending",
		TCPConn:  conn,
		Response: make(chan string),
	}

	s.mu.Lock()
	s.transfers[txID] = transfer
	s.mu.Unlock()

	sizeMB := float64(meta.Size) / (1024 * 1024)
	text := fmt.Sprintf("%s wants to send you %s (%.2f MB)\nID: %s", meta.SenderName, meta.Name, sizeMB, txID)

	cmd := exec.Command("notify-send",
		"BeamShare: Incoming file!",
		text,
		"-i", "document-send",
		"--action=accept=Accept",
		"--action=reject=Refuse",
	)
	out, err := cmd.Output()
	action := strings.TrimSpace(string(out))

	if err != nil || action != "accept" {
		s.refuseConnection(conn, txID)
		return
	}

	_, _ = conn.Write([]byte("OK\n"))
	config.AddTrustedDevices(trustedList, config.TrustedDevice{
		Name:    meta.SenderName,
		NodeId:  meta.SenderID,
		AddedAt: time.Now(),
	})

	progCmd := exec.Command("notify-send", "-p",
		"BeamShare: Downloading...",
		fmt.Sprintf("Receiving %s...", meta.Name),
		"-i", "document-receive",
	)
	progOut, _ := progCmd.Output()
	notifID := strings.TrimSpace(string(progOut))

	s.downloadFile(transfer, notifID)
}

func (s *DaemonServer) refuseConnection(conn net.Conn, txID string) {
	_, _ = conn.Write([]byte("REFUSED\n"))
	conn.Close()
	s.mu.Lock()
	delete(s.transfers, txID)
	s.mu.Unlock()
}

func (s *DaemonServer) downloadFile(t *PendingTransfer, notifID string) {
	cfg := config.Load()
	defer t.TCPConn.Close()

	rawECDH, err := crypto.DeriveSharedSecret(t.SenderID)
	if err != nil {
		fmt.Println("ECDH derivation error:", err)
		return
	}
	aesKey := sha256.Sum256(rawECDH)

	iv := make([]byte, 12)
	_, err = io.ReadFull(t.TCPConn, iv)
	if err != nil {
		fmt.Println("Unable to read IV sent by Android:", err)
		return
	}

	block, err := aes.NewCipher(aesKey[:])
	if err != nil {
		return
	}
	aesGCM, err := cipher.NewGCM(block)
	if err != nil {
		return
	}

	os.MkdirAll(cfg.DownloadPath, 0755)

	filePath := cfg.DownloadPath + "/" + t.FileName
	file, err := os.Create(filePath)
	if err != nil {
		return
	}
	defer file.Close()

	cipherText, err := io.ReadAll(t.TCPConn)
	if err != nil {
		fmt.Println("Error during byte transfer:", err)
		return
	}
	plainText, err := aesGCM.Open(nil, iv, cipherText, nil)
	if err != nil {
		fmt.Println("Decryption failed")
		return
	}

	_, _ = file.Write(plainText)

	fmt.Printf("Done! %s \n", filePath)

	s.mu.Lock()
	delete(s.transfers, t.ID)
	s.mu.Unlock()

	ext := strings.ToLower(filepath.Ext(t.FileName))
	isImage := false
	isText := false
	switch ext {
	case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp":
		isImage = true
	case ".txt", ".md", ".csv":
		isText = true
	}

	var args []string
	if notifID != "" {
		args = append(args, "-r", notifID)
	}
	args = append(args, "BeamShare: Transfer complete!", fmt.Sprintf("File received: %s", t.FileName), "-i", "document-save")

	if isImage {
		args = append(args, "--action=gallery=Open in Gallery", "--action=folder=Show in files")
	} else if isText {
		args = append(args, "--action=file=Open file", "--action=folder=Show in files")
	} else {
		args = append(args, "--action=folder=Show in files")
	}

	go func() {
		doneCmd := exec.Command("notify-send", args...)
		doneOut, _ := doneCmd.Output()
		doneAction := strings.TrimSpace(string(doneOut))

		if doneAction == "gallery" || doneAction == "file" {
			openFile(filePath)
		} else if doneAction == "folder" {
			openFolder(filepath.Dir(filePath))
		}
	}()
}

func openFile(path string) {
	if runtime.GOOS == "windows" {
		exec.Command("cmd", "/c", "start", path).Start()
	} else if runtime.GOOS == "darwin" {
		exec.Command("open", path).Start()
	} else {
		exec.Command("xdg-open", path).Start()
	}
}

func openFolder(path string) {
	if runtime.GOOS == "windows" {
		exec.Command("explorer", path).Start()
	} else if runtime.GOOS == "darwin" {
		exec.Command("open", path).Start()
	} else {
		exec.Command("xdg-open", path).Start()
	}
}

func (s *DaemonServer) registerService(port int) {
	cfg := config.Load()
	if cfg.Visibility == "hidden" {
		return
	}

	nodeID, err := crypto.GetNodeID()
	if err != nil {
		fmt.Println("Critical error during key management:", err)
		nodeID = "error_key_generation"
	}

	txtRecords := []string{
		fmt.Sprintf("device_type=%s", config.GetDeviceType()),
		fmt.Sprintf("model=%s", config.GetLinuxModel()),
		fmt.Sprintf("node_id=%s", nodeID),
	}

	utilsServices.RegisterDevice(cfg.Name, "_beamshare._tcp", "local.", port, txtRecords)
}

func (s *DaemonServer) loopCacheRefresh() {
	cfg := config.Load()
	go func() {
		time.Sleep(1 * time.Second)
		if cfg.EveryoneModeUntil != nil {
			if time.Now().After(*cfg.EveryoneModeUntil) {
				cfg.EveryoneModeUntil = nil
				cfg.Visibility = "trusted"
				config.Save(cfg)
			}
		}
		for {
			list, err := utilsServices.FetchDevices("_beamshare._tcp", 4, false)
			if err == nil && len(list) > 0 {
				s.mergeCache(list)
			}

			time.Sleep(10 * time.Second)
		}
	}()
}

func (s *DaemonServer) mergeCache(newList []utilsServices.DeviceInfo) {
	s.mu.Lock()
	defer s.mu.Unlock()

	now := time.Now()
	for _, d := range newList {
		s.devices[d.Ip] = CachedDevice{
			Info:     d,
			LastSeen: now,
		}
	}

	for ip, dev := range s.devices {
		if now.Sub(dev.LastSeen) > 60*time.Second {
			delete(s.devices, ip)
		}
	}
}

func (s *DaemonServer) scanNetworkActive() ([]utilsServices.DeviceInfo, error) {
	return utilsServices.FetchDevices("_beamshare._tcp", 3, DebugMode)
}

func (s *DaemonServer) listenIPC() {
	os.Remove(socketPath)

	l, err := net.Listen("unix", socketPath)
	if err != nil {
		logDebug("Unable to create UNIX socket: " + err.Error())
		return
	}

	s.listener = l
	defer srvClose(s.listener)

	for {
		con, err := s.listener.Accept()
		if err != nil {
			continue
		}

		go s.handleConnection(con)
	}
}

func srvClose(l net.Listener) {
	l.Close()
	os.Remove(socketPath)
}

func (s *DaemonServer) handleConnection(conn net.Conn) {
	defer conn.Close()
	decoder := json.NewDecoder(conn)
	encoder := json.NewEncoder(conn)

	var msg MessageIPC
	if err := decoder.Decode(&msg); err != nil {
		return
	}

	switch msg.Event {
	case "list_devices":
		s.mu.RLock()
		var list []utilsServices.DeviceInfo
		for _, d := range s.devices {
			list = append(list, utilsServices.DeviceInfo{
				Name:   d.Info.Name,
				Ip:     d.Info.Ip,
				Port:   d.Info.Port,
				NodeId: d.Info.NodeId,
			})
		}
		s.mu.RUnlock()
		_ = encoder.Encode(list)

	case "list_transfers":
		s.mu.RLock()
		var list []PendingTransfer
		for _, t := range s.transfers {
			if t.Status == "pending" {
				list = append(list, *t)
			}
		}
		s.mu.RUnlock()
		_ = encoder.Encode(list)

	case "attach_transfer":
		s.mu.Lock()
		t, exists := s.transfers[msg.ID]
		s.mu.Unlock()

		if !exists {
			_ = encoder.Encode(MessageIPC{Event: "error", Value: "Unknown ID"})
			return
		}

		_ = encoder.Encode(MessageIPC{Event: "incoming_req", ID: t.ID, File: t.FileName, Size: t.Size, From: t.Sender, NodeId: t.SenderID})

		var resp MessageIPC
		if err := decoder.Decode(&resp); err == nil {
			if resp.Value == "accept" {
				s.mu.Lock()
				t.Status = "transferring"
				s.mu.Unlock()
				t.Response <- "accept"
				_ = encoder.Encode(MessageIPC{Event: "done"})
			} else {
				t.Response <- "reject"
			}
		}
	}
}

func logDebug(message string) {
	if DebugMode {
		fmt.Println(message)
	}
}
