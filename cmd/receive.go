package cmd

import (
	"beam-share-cli/internal/daemon"
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"strings"

	"github.com/spf13/cobra"
)

var receiveCmd = &cobra.Command{
	Use:   "receive [id]",
	Short: "Manage incoming files",
	Args:  cobra.MaximumNArgs(1),
	Run:   runReceive,
}

func init() { rootCmd.AddCommand(receiveCmd) }

func runReceive(cmd *cobra.Command, args []string) {
	conn, err := net.Dial("unix", "/tmp/beamshare.sock")
	if err != nil {
		fmt.Println("Error: Daemon inactive.")
		return
	}
	defer conn.Close()

	encoder := json.NewEncoder(conn)
	decoder := json.NewDecoder(conn)

	if len(args) == 0 {
		_ = encoder.Encode(daemon.MessageIPC{Event: "list_transfers"})
		var list []daemon.PendingTransfer
		_ = decoder.Decode(&list)

		if len(list) == 0 {
			fmt.Println("No file waiting to be received.")
			return
		}

		fmt.Println("ID\t\tSENDER\t\tFILE\t\tSIZE")
		for _, t := range list {
			fmt.Printf("%s\t%s\t%s\t%.2f MB\n", t.ID, t.Sender, t.FileName, float64(t.Size)/(1024*1024))
		}
		return
	}

	_ = encoder.Encode(daemon.MessageIPC{Event: "attach_transfer", ID: args[0]})
	var msg daemon.MessageIPC
	_ = decoder.Decode(&msg)

	if msg.Event == "error" {
		fmt.Println("Error:", msg.Value)
		return
	}

	fmt.Printf("\nFile: %s from %s\nAccept? [y/n]: ", msg.File, msg.From)
	reader := bufio.NewReader(os.Stdin)
	input, _ := reader.ReadString('\n')
	choice := strings.ToLower(strings.TrimSpace(input))

	val := "reject"
	if choice == "y" || choice == "o" {
		val = "accept"
	}

	_ = encoder.Encode(daemon.MessageIPC{Value: val})

	if val == "accept" {
		fmt.Println("Downloading to your Downloads folder...")
		var finalMsg daemon.MessageIPC
		_ = decoder.Decode(&finalMsg)
		fmt.Println("Done! self-correcting flow")
	} else {

	}
}
