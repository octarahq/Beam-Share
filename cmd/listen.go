package cmd

import (
	"beam-share-cli/internal/daemon"
	"encoding/json"
	"fmt"
	"net"
	"github.com/spf13/cobra"
)

var listenEventsCmd = &cobra.Command{
	Use:   "listen-events",
	Short: "Listens to daemon events and prints them as JSON for the GUI",
	Run: func(cmd *cobra.Command, args []string) {
		conn, err := net.Dial("unix", "/tmp/beamshare.sock")
		if err != nil {
			fmt.Println(`{"status": "error", "message": "Daemon not started"}`)
			return
		}
		defer conn.Close()

		encoder := json.NewEncoder(conn)
		decoder := json.NewDecoder(conn)

		_ = encoder.Encode(daemon.MessageIPC{Event: "subscribe_events"})

		for {
			var event daemon.MessageIPC
			if err := decoder.Decode(&event); err != nil {
				break
			}

			jsonBytes, _ := json.Marshal(event)
			fmt.Println(string(jsonBytes))
		}
	},
}

func init() {
	rootCmd.AddCommand(listenEventsCmd)
}