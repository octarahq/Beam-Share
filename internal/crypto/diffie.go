package crypto

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/x509"
	"encoding/hex"
	"encoding/pem"
	"fmt"
	"math/big"
	"os"
	"strings"
)

func DeriveSharedSecret(senderPubKeyHex string) ([]byte, error) {
	home, _ := os.UserHomeDir()
	pemBytes, err := os.ReadFile(home + "/.config/beamshare/id_ecdsa.pem")
	if err != nil {
		return nil, fmt.Errorf("unable to read PC private key: %w", err)
	}
	block, _ := pem.Decode(pemBytes)
	if block == nil {
		return nil, fmt.Errorf("invalid private key PEM block")
	}
	privKey, err := x509.ParseECPrivateKey(block.Bytes)
	if err != nil {
		return nil, fmt.Errorf("private key parse error: %w", err)
	}

	cleanedHex := strings.TrimSpace(senderPubKeyHex)
	cleanedHex = strings.ReplaceAll(cleanedHex, "\n", "")
	cleanedHex = strings.ReplaceAll(cleanedHex, "\r", "")
	pubBytes, err := hex.DecodeString(cleanedHex)
	if err != nil {
		return nil, fmt.Errorf("hex decoding error: %w", err)
	}

	if len(pubBytes) < 64 {
		return nil, fmt.Errorf("public key data too short (%d bytes)", len(pubBytes))
	}

	rawX := pubBytes[len(pubBytes)-64 : len(pubBytes)-32]
	rawY := pubBytes[len(pubBytes)-32:]

	curve := elliptic.P256()
	x := new(big.Int).SetBytes(rawX)
	y := new(big.Int).SetBytes(rawY)

	if !curve.IsOnCurve(x, y) {
		return nil, fmt.Errorf("extracted coordinates are not a valid point on the P-256 curve")
	}

	senderPubKey := &ecdsa.PublicKey{
		Curve: curve,
		X:     x,
		Y:     y,
	}

	ecdhPriv, err := privKey.ECDH()
	if err != nil {
		return nil, fmt.Errorf("error converting private key to ECDH: %w", err)
	}
	ecdhPub, err := senderPubKey.ECDH()
	if err != nil {
		return nil, fmt.Errorf("error converting public key to ECDH: %w", err)
	}

	secret, err := ecdhPriv.ECDH(ecdhPub)
	if err != nil {
		return nil, fmt.Errorf("error calculating shared secret: %w", err)
	}

	return secret, nil
}
