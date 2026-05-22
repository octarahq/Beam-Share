package network

import (
	"beam-share-cli/internal/config"
	"beam-share-cli/internal/crypto"
	"crypto/aes"
	"crypto/cipher"
	"crypto/ecdsa"
	"crypto/rand"
	"crypto/sha256"
	"crypto/x509"
	"encoding/hex"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"io"
	"net"
	"os"
	"path/filepath"
	"strings"
	"time"
)

func SendFileToDevice(targetIP string, targetPort int, filePath string, remotePubKeyHex string) error {
	file, err := os.Open(filePath)
	if err != nil {
		return err
	}
	defer file.Close()

	fileInfo, err := file.Stat()
	if err != nil {
		return err
	}

	cfg := config.Load()
	myPubKeyHex, _ := crypto.GetNodeID()

	meta := map[string]interface{}{
		"name":                 filepath.Base(filePath),
		"size":                 fileInfo.Size(),
		"type":                 "application/octet-stream",
		"sender_id":            myPubKeyHex,
		"sender_name":          cfg.Name,
		"encryption_supported": true,
	}

	conn, err := net.DialTimeout("tcp", fmt.Sprintf("%s:%d", targetIP, targetPort), 5*time.Second)
	if err != nil {
		return fmt.Errorf("unable to connect to device: %w", err)
	}
	defer conn.Close()

	encoder := json.NewEncoder(conn)
	if err := encoder.Encode(meta); err != nil {
		return err
	}

	var challengeBuf = make([]byte, 1024)
	n, err := conn.Read(challengeBuf)
	if err != nil {
		return fmt.Errorf("error reading Android challenge: %w", err)
	}
	challenge := string(challengeBuf[:n])

	signatureHex, err := SignChallenge(challenge)
	if err != nil {
		return fmt.Errorf("unable to sign challenge: %w", err)
	}

	_, _ = conn.Write([]byte(signatureHex + "\n"))

	fmt.Println("Request sent. Waiting for validation on the device...")
	var authBuf = make([]byte, 1024)
	n, err = conn.Read(authBuf)
	if err != nil {
		return fmt.Errorf("device closed the connection: %w", err)
	}

	response := strings.TrimSpace(string(authBuf[:n]))

	if response != "OK" {
		return fmt.Errorf("transfer refused by device (received response: %q)", response)
	}
	fmt.Println("Transfer accepted! Encrypting stream...")

	rawECDH, err := crypto.DeriveSharedSecret(remotePubKeyHex)
	if err != nil {
		return fmt.Errorf("ECDH derivation error: %w", err)
	}
	aesKey := sha256.Sum256(rawECDH)

	block, err := aes.NewCipher(aesKey[:])
	if err != nil {
		return err
	}
	aesGCM, err := cipher.NewGCM(block)
	if err != nil {
		return err
	}

	iv := make([]byte, 12)
	_, _ = rand.Read(iv)
	_, _ = conn.Write(iv)

	plainText, err := io.ReadAll(file)
	if err != nil {
		return err
	}

	cipherText := aesGCM.Seal(nil, iv, plainText, nil)

	_, err = conn.Write(cipherText)
	if err != nil {
		return fmt.Errorf("error during byte transfer: %w", err)
	}

	fmt.Println("File sent successfully!")
	return nil
}

func SignChallenge(challenge string) (string, error) {
	keyPath := crypto.GetPathKey()
	pemBytes, err := os.ReadFile(keyPath)
	if err != nil {
		return "", err
	}

	block, _ := pem.Decode(pemBytes)
	privKey, err := x509.ParseECPrivateKey(block.Bytes)
	if err != nil {
		return "", err
	}

	cleanChallenge := strings.TrimRight(challenge, "\r\n")

	var hashToSign [32]byte
	decodedChallenge, errDecode := hex.DecodeString(cleanChallenge)
	if errDecode == nil && len(decodedChallenge) > 0 {
		hashToSign = sha256.Sum256(decodedChallenge)
	} else {
		hashToSign = sha256.Sum256([]byte(cleanChallenge))
	}

	sigBytes, err := ecdsa.SignASN1(rand.Reader, privKey, hashToSign[:])
	if err != nil {
		return "", err
	}

	return hex.EncodeToString(sigBytes), nil
}
