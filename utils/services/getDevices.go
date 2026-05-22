package utilsServices

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/grandcat/zeroconf"
)

type DeviceInfo struct {
	Name   string `json:"name"`
	Ip     string `json:"ip"`
	Port   int    `json:"port"`
	NodeId string `json:"node_id"`
}

func FetchDevices(serviceName string, scanTime time.Duration, debug bool) ([]DeviceInfo, error) {
	resolver, err := zeroconf.NewResolver(nil)
	if err != nil {
		return nil, err
	}

	ctx, cancel := context.WithTimeout(context.Background(), scanTime*time.Second)
	defer cancel()

	entries := make(chan *zeroconf.ServiceEntry)
	var devices []DeviceInfo
	collectDone := make(chan struct{})

	go func(results <-chan *zeroconf.ServiceEntry) {
		for entry := range results {
			ip := "Unknown"
			if len(entry.AddrIPv4) > 0 {
				ip = entry.AddrIPv4[0].String()
			}

			device := DeviceInfo{
				Name:   ParseName(entry.Instance),
				Ip:     ip,
				Port:   entry.Port,
				NodeId: getTXTValue(entry.Text, "node_id"),
			}
			devices = append(devices, device)
		}
		close(collectDone)
	}(entries)

	if debug {
		fmt.Printf("Search for BeamShare devices for %d seconds...\n", scanTime)
	}

	err = resolver.Browse(ctx, serviceName, "local.", entries)
	if err != nil {
		return nil, err
	}

	<-ctx.Done()
	<-collectDone

	if devices == nil {
		devices = []DeviceInfo{}
	}

	if debug {
		fmt.Printf("Found %d devices :", len(devices))
		for i, d := range devices {
			fmt.Printf("\n#%d %s %s:%d (node_id: %s)", i+1, d.Name, d.Ip, d.Port, d.NodeId)
		}
		fmt.Println("")
	}

	return devices, nil
}

func getTXTValue(txt []string, key string) string {
	prefix := key + "="
	for _, record := range txt {
		if strings.HasPrefix(record, prefix) {
			return strings.TrimPrefix(record, prefix)
		}
	}
	return ""
}
