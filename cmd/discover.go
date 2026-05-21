package cmd

import (
	utilsServices "beam-share-cli/utils/services"
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/grandcat/zeroconf"
	"github.com/spf13/cobra"
)

var isJsonOutput bool
var scanTime int16

var discoverCmd = &cobra.Command{
	Use:   "discover",
	Short: "Discover devices on your network",
	Long: `Discover BeamShare devices advertised on your local network using mDNS/zeroconf.

By default, results are printed in a human-readable format.
Use --json for machine-readable output and --scan-time to control how long the discovery runs.`,
	Run: run,
}

func init() {
	rootCmd.AddCommand(discoverCmd)

	discoverCmd.Flags().BoolVarP(&isJsonOutput, "json", "j", false, "Output results in JSON format")
	discoverCmd.Flags().Int16VarP(&scanTime, "scan-time", "s", 3, "Set scan duration in seconds (default: 3)")
}

type DeviceInfo struct {
	Name string `json:"name"`
	Ip   string `json:"ip"`
	Port int    `json:"port"`
}

func run(cmd *cobra.Command, args []string) {
	resolver, err := zeroconf.NewResolver(nil)
	if err != nil {
		if isJsonOutput {
			fmt.Printf(`{"error":"%s"}`+"\n", err.Error())
		} else {
			fmt.Println("Failed to initialize resolver:", err.Error())
		}
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), time.Duration(scanTime)*time.Second)
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
				Name: utilsServices.ParseName(entry.Instance),
				Ip:   ip,
				Port: entry.Port,
			}
			devices = append(devices, device)

			if !isJsonOutput {
				fmt.Printf("Device found : %s (%s:%d)\n", device.Name, device.Ip, device.Port)
			}
		}
		close(collectDone)
	}(entries)

	if !isJsonOutput {
		fmt.Printf("Search for BeamShare devices for %d seconds...\n", scanTime)
	}

	err = resolver.Browse(ctx, "_beamshare._tcp", "local.", entries)
	if err != nil {
		if isJsonOutput {
			fmt.Printf(`{"error":"%s"}`+"\n", err.Error())
		} else {
			fmt.Println("Failed to browse:", err.Error())
		}
		return
	}

	<-ctx.Done()

	<-collectDone

	if isJsonOutput {
		if devices == nil {
			devices = []DeviceInfo{}
		}

		jsonData, err := json.Marshal(devices)
		if err != nil {
			fmt.Printf(`{"error":"Failed to generate JSON: %s"}`+"\n", err.Error())
			return
		}

		fmt.Println(string(jsonData))
	} else {
		fmt.Println("Scan finished.")
	}
}
