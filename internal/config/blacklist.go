package config

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"time"
)

type BlackListDevice struct {
	NodeId  string    `json:"node_id"`
	Name    string    `json:"name"`
	AddedAt time.Time `json:"added_at"`
}

type Blacklist []BlackListDevice

func GetBlackListPath() string {
	home, _ := os.UserHomeDir()
	return filepath.Join(home, ".config", "beamshare", "blacklist.json")
}

func LoadBlackList() Blacklist {
	var blacklist Blacklist
	path := GetBlackListPath()
	data, err := os.ReadFile(path)
	if err != nil {
		return Blacklist{}
	}

	err = json.Unmarshal(data, &blacklist)
	if err != nil {
		SaveBlackList(Blacklist{})
		return Blacklist{}
	}
	return blacklist
}

func SaveBlackList(blacklist Blacklist) error {
	data, err := json.MarshalIndent(blacklist, "", " ")
	if err != nil {
		return err
	}

	path := GetBlackListPath()

	return os.WriteFile(path, data, 0755)
}

func AddDevice(blacklist Blacklist, name string, nodeid string) error {
	device := BlackListDevice{
		Name:    name,
		NodeId:  nodeid,
		AddedAt: time.Now(),
	}
	blacklist = append(blacklist, device)

	return SaveBlackList(blacklist)
}

func RemoveDevice(blacklist Blacklist, nodeid string) error {
	for i, device := range blacklist {
		if device.NodeId == nodeid {
			// remove the device from the array
			blacklist = append(blacklist[:i], blacklist[i+1:]...)
		}
	}

	return SaveBlackList(blacklist)
}

func DeviceInBlackList(blacklist Blacklist, nodeid string) bool {
	nodeid = strings.TrimSpace(strings.ToLower(nodeid))
	for _, d := range blacklist {
		id := strings.TrimSpace(strings.ToLower(d.NodeId))
		if id == nodeid {
			return true
		}
	}

	return false
}
