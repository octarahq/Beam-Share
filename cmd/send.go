package cmd

import (
	"beam-share-cli/internal/daemon"
	"beam-share-cli/internal/network"
	utilsServices "beam-share-cli/utils/services"
	"encoding/json"
	"fmt"
	"net"
	"strings"

	"github.com/spf13/cobra"
)

var targetIP string
var targetPort int

var sendCmd = &cobra.Command{
	Use:   "send [file_path]",
	Short: "Send a file to a device",
	Args:  cobra.ExactArgs(1),
	Run: func(cmd *cobra.Command, args []string) {
		filePath := args[0]

		if targetIP == "" {
			fmt.Println("Error: Target IP address (--to) is required.")
			return
		}

		conn, err := net.Dial("unix", "/tmp/beamshare.sock")
		var resolvedKey string

		if err == nil {
			defer conn.Close()
			encoder := json.NewEncoder(conn)
			decoder := json.NewDecoder(conn)

			_ = encoder.Encode(daemon.MessageIPC{Event: "list_devices"})
			var devices []utilsServices.DeviceInfo
			if err := decoder.Decode(&devices); err == nil {
				for _, d := range devices {
					if d.Ip == targetIP || strings.Contains(d.Ip, targetIP) {
						resolvedKey = d.NodeId
						if targetPort == 0 {
							targetPort = d.Port
						}
						break
					}
				}
			}
		}

		if resolvedKey == "" {
			devices, err := utilsServices.FetchDevices("_beamshare._tcp", 3, false)
			if err == nil {
				for _, d := range devices {
					if d.Ip == targetIP || strings.Contains(d.Ip, targetIP) {
						resolvedKey = d.NodeId
						if targetPort == 0 {
							targetPort = d.Port
						}
						break
					}
				}
			}
		}

		if resolvedKey == "" {
			fmt.Printf("Error: Unable to find device with IP %s on the local network.\n", targetIP)
			return
		}

		if targetPort == 0 {
			targetPort = 37865
		}

		fmt.Printf("Initializing transfer of %s to %s:%d...\n", filePath, targetIP, targetPort)

		err = network.SendFileToDevice(targetIP, targetPort, filePath, resolvedKey)
		if err != nil {
			fmt.Println("\nTransfer failed:", err)
		}
	},
}

func init() {
	rootCmd.AddCommand(sendCmd)
	sendCmd.Flags().StringVarP(&targetIP, "to", "t", "", "Target device IP address")
	sendCmd.Flags().IntVarP(&targetPort, "port", "p", 0, "TCP port (optional, resolved via mDNS)")
}
