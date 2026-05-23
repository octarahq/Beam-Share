package cmd

import (
	"beam-share-cli/internal/config"
	"beam-share-cli/utils"
	"fmt"
	"os"
	"strconv"
	"strings"
	"text/tabwriter"
	"time"

	"github.com/spf13/cobra"
)

var setValue string
var removeIdx int
var reset bool
var Time string

var configCmd = &cobra.Command{
	Use:   "config",
	Short: "Manage BeamShare configuration",
	Long: `Exemple of use : beamshare config name --set MyPC
	
Available settings:
	- name
	- downloadpath
	- blacklist
	- visibility
`,
	Run: func(cmd *cobra.Command, args []string) {
		cfg := config.Load()
		bl := config.LoadBlackList()

		if len(args) == 0 {
			fmt.Printf("name = %s\n", cfg.Name)
			fmt.Printf("downloadpath = %s\n", cfg.DownloadPath)
			if cfg.EveryoneModeUntil != nil {
				remaining := time.Until(*cfg.EveryoneModeUntil)
				fmt.Printf("visibility = %s (%02d:%02d)", cfg.Visibility, int(remaining.Minutes()), int(remaining.Hours()))
			} else {
				fmt.Printf("visibility = %s\n", cfg.Visibility)
			}
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
				if reset {
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
		case "visibility":
			if cmd.Flags().Changed("set") {
				if cmd.Flags().Changed("time") {
					parts := strings.Split(Time, ":")

					minutes, _ := strconv.Atoi(parts[0])
					seconds, _ := strconv.Atoi(parts[1])

					duration := time.Duration(minutes)*time.Minute +
						time.Duration(seconds)*time.Second

					until := time.Now().Add(duration)
					cfg.EveryoneModeUntil = &until
					cfg.Visibility = "all"
				}

				possiblesChoices := []string{"disabled", "trusted", "all"}
				if !utils.Contains(possiblesChoices, setValue) {
					fmt.Println("Invalid mode :", setValue)
					return
				}
				cfg.Visibility = setValue

				config.Save(cfg)
				fmt.Println("Saved!")
			} else {
				fmt.Println("You can change your visibility with 'beamshare config visibility --set ...' :")
				fmt.Println("\t- disabled (nobody can see your device, you can still send file)")
				fmt.Println("\t- trusted  (all devices can see you but all their request will be rejected if not in the trusted devices list)")
				fmt.Println("\t- all --time min:sec (everybody can see you for min and sec)")
				fmt.Println("\t- all (everybody can see you)")
				fmt.Println("")
				if cfg.EveryoneModeUntil != nil {
					remaining := time.Until(*cfg.EveryoneModeUntil)
					fmt.Printf("visibility = %s (%02d:%02d)\n", cfg.Visibility, int(remaining.Minutes()), int(remaining.Hours()))
				} else {
					fmt.Printf("visibility = %s\n", cfg.Visibility)
				}
			}
		default:
			fmt.Printf("Unknown configuration key : %s", key)
		}

		fmt.Println("")
		fmt.Println("If you made a change, you may need a 'beamshare daemon resatart &' for them to apply")
	},
}

func init() {
	rootCmd.AddCommand(configCmd)

	configCmd.Flags().StringVarP(&setValue, "set", "s", "", "New settings")
	configCmd.Flags().IntVar(&removeIdx, "remove", 0, "Remove a device from the blacklist")
	configCmd.Flags().BoolVarP(&reset, "reset", "r", false, "Reset to default value")
	configCmd.Flags().StringVar(&Time, "time", "10:00", "Change the time")
}
