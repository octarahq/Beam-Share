package config

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"time"
)

type TrustedDevice struct {
	Name    string        `json:"name"`
	NodeId  string        `json:"node_id"`
	AddedAt time.Duration `json:"added_at"`
}

type TrustedDevices []TrustedDevice

func GetTrustedDevicesPath() string {
	home, _ := os.UserHomeDir()
	return filepath.Join(home, ".config", "beamshare", "trusted_devices.json")
}

func LoadTrustedDevices() TrustedDevices {
	path := GetTrustedDevicesPath()
	data, err := os.ReadFile(path)
	if err != nil {
		return TrustedDevices{}
	}

	var devices TrustedDevices
	err = json.Unmarshal(data, &devices)
	if err != nil {
		return TrustedDevices{}
	}

	return devices
}

func AddTrustedDevices(list TrustedDevices, device TrustedDevice) error {
	list = append(list, device)

	return SaveTrustedDevices(list)
}

func SaveTrustedDevices(list TrustedDevices) error {
	data, err := json.MarshalIndent(list, "", " ")
	if err != nil {
		return err
	}

	path := GetTrustedDevicesPath()

	return os.WriteFile(path, data, 0755)
}

func IsTrusted(list TrustedDevices, nodeid string) bool {
	nodeid = strings.TrimSpace(strings.ToLower(nodeid))
	for _, d := range list {
		id := strings.TrimSpace(strings.ToLower(d.NodeId))
		if id == nodeid {
			return true
		}
	}

	return false
}
