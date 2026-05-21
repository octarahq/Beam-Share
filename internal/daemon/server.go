package daemon

import (
	utilsServices "beam-share-cli/utils/services"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"sync"
	"time"
)

const socketPath = "/tmp/beamshare.sock"

type CachedDevice struct {
	Info     utilsServices.DeviceInfo
	LastSeen time.Time
}

type DaemonServer struct {
	mu       sync.RWMutex
	devices  map[string]CachedDevice
	listener net.Listener
}

func StartServer() {
	srv := &DaemonServer{
		devices: make(map[string]CachedDevice),
	}

	go srv.loopCacheRefresh()

	srv.listenIPC()
}

func (s *DaemonServer) loopCacheRefresh() {
	for i := 0; i < 3; i++ {
		go func(delay time.Duration) {
			time.Sleep(delay)

			for {
				list, err := s.scanNetworkActive()
				if err == nil && len(list) > 0 {
					s.mergeCache(list)
				}
				time.Sleep(2 * time.Second)
			}
		}(time.Duration(i) * time.Second)
	}
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
		if now.Sub(dev.LastSeen) > 10*time.Second {
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

	s.mu.RLock()
	var list []utilsServices.DeviceInfo
	for _, dev := range s.devices {
		list = append(list, dev.Info)
	}
	s.mu.RUnlock()

	if list == nil {
		list = []utilsServices.DeviceInfo{}
	}

	jsonData, _ := json.Marshal(list)
	conn.Write(jsonData)
}

func logDebug(message string) {
	if DebugMode {
		fmt.Println(message)
	}
}
