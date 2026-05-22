package config

import (
	"os"
	"strings"
)

func GetDeviceType() string {
	return "pc"
}

func GetLinuxModel() string {
	if data, err := os.ReadFile("/sys/devices/virtual/dmi/id/product_name"); err == nil {
		model := strings.TrimSpace(string(data))
		if model != "" {
			return model
		}
	}

	if data, err := os.ReadFile("/etc/os-release"); err == nil {
		lines := strings.Split(string(data), "\n")
		for _, line := range lines {
			if strings.HasPrefix(line, "PRETTY_NAME=") {
				name := strings.Trim(line[12:], `"`)
				return name
			}
		}
	}

	return "Linux Device"
}
