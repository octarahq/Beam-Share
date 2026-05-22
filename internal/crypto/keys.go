package crypto

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/x509"
	"encoding/hex"
	"encoding/pem"
	"os"
	"path/filepath"
)

func GetPathKey() string {
	home, _ := os.UserHomeDir()

	return filepath.Join(home, ".config", "beamshare", "id_ecdsa.pem")
}

func GetNodeID() (string, error) {
	keyPath := GetPathKey()
	var privKey *ecdsa.PrivateKey

	if _, err := os.Stat(keyPath); os.IsNotExist(err) {
		var errGen error
		privKey, errGen = ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
		if errGen != nil {
			return "", errGen
		}

		derBytes, errMarshal := x509.MarshalECPrivateKey(privKey)
		if errMarshal != nil {
			return "", errMarshal
		}

		_ = os.MkdirAll(filepath.Dir(keyPath), 0700)

		file, errCreate := os.OpenFile(keyPath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0600)
		if errCreate != nil {
			return "", errCreate
		}
		defer file.Close()

		_ = pem.Encode(file, &pem.Block{Type: "EC PRIVATE KEY", Bytes: derBytes})
	} else {
		pemBytes, errRead := os.ReadFile(keyPath)
		if errRead != nil {
			return "", errRead
		}

		block, _ := pem.Decode(pemBytes)
		var errParse error
		privKey, errParse = x509.ParseECPrivateKey(block.Bytes)
		if errParse != nil {
			return "", errParse
		}
	}

	pubKey := &privKey.PublicKey

	pubBytes, errPub := x509.MarshalPKIXPublicKey(pubKey)
	if errPub != nil {
		return "", errPub
	}

	return hex.EncodeToString(pubBytes), nil
}
