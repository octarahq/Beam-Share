package cmd

import (
	"beam-share-cli/internal/config"
	"fmt"
	"os"
	"strings"
	"text/tabwriter"

	"github.com/spf13/cobra"
)

var setValue string
var removeIdx int
var reset bool

var configCmd = &cobra.Command{
	Use:   "config",
	Short: "Manage BeamShare configuration",
	Long: `Exemple of use : config name --set MyPC
	
Available settings:
	- name
	- downloadpath
	- blacklist
`,
	Run: func(cmd *cobra.Command, args []string) {
		cfg := config.Load()

		if len(args) == 0 {
			fmt.Printf("name = %s\n", cfg.Name)
			fmt.Printf("downloadpath = %s\n", cfg.DownloadPath)
			return
		}

		key := strings.ToLower(args[0])

		switch key {
		case "name":
			if cmd.Flags().Changed("set") || cmd.Flags().Changed("reset") {
				if setValue == "DEFAULT_VALUE" || reset {
					cfg.Name, _ = os.Hostname()
				} else {
					cfg.Name = setValue
				}
				err := config.Save(cfg)
				if err != nil {
					fmt.Println("Error while saving :", err)
				} else {
					fmt.Println("Saved!")
				}
			} else {
				fmt.Println(cfg.Name)
			}
		case "downloadpath":
			if cmd.Flags().Changed("set") || cmd.Flags().Changed("reset") {
				if setValue == "DEFAULT_VALUE" || reset {
					home, _ := os.UserHomeDir()
					cfg.DownloadPath = home + "/Downloads/BeamShare"
					if _, err := os.Stat(home + "/Téléchargements"); err == nil {
						cfg.DownloadPath = home + "/Téléchargements/BeamShare"
					}
				} else {
					cfg.DownloadPath = setValue
				}
				err := config.Save(cfg)
				if err != nil {
					fmt.Println("Error while saving :", err)
				} else {
					fmt.Println("Saved!")
				}
			} else {
				fmt.Println(cfg.DownloadPath)
			}
		case "blacklist":
			bl := config.LoadBlackList()
			if cmd.Flags().Changed("remove") {
				device := bl[removeIdx]
				err := config.RemoveDevice(bl, device.NodeId)
				if err != nil {
					fmt.Println("Error while removing the device :", err)
					return
				}
				fmt.Println("Succes!")
			} else {
				fmt.Println("All the blacklisted devices :")
				fmt.Println("[Tip] Use --remove [id] to remove the device you want.")
				fmt.Println("")
				w := tabwriter.NewWriter(os.Stdout, 0, 0, 3, ' ', 0)

				fmt.Fprintln(w, "ID\tNAME\tADDED AT\tPUBLIC KEY")

				for i, d := range bl {
					dateStr := d.AddedAt.Format("Mon Jan 2 15:04:05 2006")

					shortKey := d.NodeId
					if len(shortKey) > 12 {
						shortKey = shortKey[:12] + "..."
					}

					fmt.Fprintf(w, "%d\t%s\t%s\t%s\n", i, d.Name, dateStr, shortKey)
				}

				w.Flush()
			}
		default:
			fmt.Printf("Unknown configuration key : %s", key)
		}
	},
}

func init() {
	rootCmd.AddCommand(configCmd)

	configCmd.Flags().StringVarP(&setValue, "set", "s", "", "New settings")
	configCmd.Flags().Lookup("set").NoOptDefVal = "DEFAULT_VALUE"
	configCmd.Flags().IntVar(&removeIdx, "remove", 0, "Remove a device from the blacklist")
	configCmd.Flags().BoolVarP(&reset, "reset", "r", false, "Reset to default value")
}
