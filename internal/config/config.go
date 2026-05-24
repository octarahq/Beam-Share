package config

import (
	"encoding/json"
	"os"
	"path/filepath"
	"time"
)

type AppConfig struct {
	Name              string     `json:"name"`
	DownloadPath      string     `json:"download_path"`
	Visibility        string     `json:"visibility"` // disabled | trusted | all
	EveryoneModeUntil *time.Time `json:"everyone_mode_until"`
	StartOnBoot       bool       `json:"start_on_boot"`
	AutoAccept        bool       `json:"auto_accept"`
	LastPort          int        `json:"last_port"`
}

func getConfigFilepath() string {
	home, _ := os.UserHomeDir()
	return filepath.Join(home, ".config", "beamshare", "config.json")
}

func Load() AppConfig {
	var cfg AppConfig
	var err error
	cfg.Name, err = os.Hostname()

	home, _ := os.UserHomeDir()
	cfg.DownloadPath = home + "/Downloads/BeamShare"
	if _, err := os.Stat(home + "/Téléchargements"); err == nil {
		cfg.DownloadPath = home + "/Téléchargements/BeamShare"
	}
	cfg.EveryoneModeUntil = nil
	cfg.Visibility = "disabled"
	cfg.AutoAccept = false

	if err != nil {
		return AppConfig{}
	}

	file := getConfigFilepath()
	data, err := os.ReadFile(file)
	if err != nil {
		return cfg
	}

	_ = json.Unmarshal(data, &cfg)

	return cfg
}

func Save(cfg AppConfig) error {
	file := getConfigFilepath()

	err := os.MkdirAll(filepath.Dir(file), 0755)
	if err != nil {
		return err
	}

	data, err := json.MarshalIndent(cfg, "", " ")
	if err != nil {
		return err
	}

	return os.WriteFile(file, data, 0644)
}
