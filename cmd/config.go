package cmd

import (
	"beam-share-cli/internal/config"
	"beam-share-cli/internal/daemon"
	"beam-share-cli/utils"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
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
var Enable bool
var Disable bool

var configCmd = &cobra.Command{
	Use:   "config",
	Short: "Manage BeamShare configuration",
	Long: `Exemple of use : beamshare config name --set MyPC
	
Available settings:
	- name
	- downloadpath
	- blacklist
	- visibility
	- startonboot
	- autoaccept
`,
	Run: func(cmd *cobra.Command, args []string) {
		cfg := config.Load()
		bl := config.LoadBlackList()

		if len(args) == 0 {
			fmt.Printf("name = %s\n", cfg.Name)
			fmt.Printf("downloadpath = %s\n", cfg.DownloadPath)
			if cfg.EveryoneModeUntil != nil {
				remaining := time.Until(*cfg.EveryoneModeUntil)
				fmt.Printf("visibility = %s (%02d:%02d)\n", cfg.Visibility, int(remaining.Minutes()), int(remaining.Hours()))
			} else {
				fmt.Printf("visibility = %s\n", cfg.Visibility)
			}
			fmt.Printf("startonboot = %t\n", cfg.StartOnBoot)
			fmt.Printf("autoaccept = %t\n", cfg.AutoAccept)
			return
		}

		if cmd.Flags().Changed("enabled") && cmd.Flags().Changed("disabled") {
			fmt.Println("Error: You cannot use enabled and disable flags at th same time!")
			return
		}

		if cmd.Flags().Changed("reset") && cmd.Flags().Changed("set") {
			fmt.Println("Error: You cannot use reset and set flags at th same time!")
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
		case "startonboot":
			if cmd.Flags().Changed("set") || cmd.Flags().Changed("reset") {
				var targetVal bool
				if reset {
					targetVal = false
				} else {
					targetVal = strings.ToLower(setValue) == "true" || setValue == "1"
				}
				cfg.StartOnBoot = targetVal
				err := config.Save(cfg)
				if err != nil {
					fmt.Println("Error while saving :", err)
					return
				}
				err = manageSystemdService(targetVal)
				if err != nil {
					fmt.Println("Error managing systemd service:", err)
				} else {
					fmt.Println("Saved and systemd service updated!")
				}
			} else {
				fmt.Println(cfg.StartOnBoot)
			}
		case "autoaccept":
			if cmd.Flags().Changed("enabled") || cmd.Flags().Changed("disabled") {
				var state bool
				if Enable {
					state = true
				} else if Disable {
					state = false
				}

				cfg.AutoAccept = state
				config.Save(cfg)
				fmt.Println("Success!")
			} else {
				if cfg.AutoAccept {
					fmt.Println("Feature enabled")
				} else {
					fmt.Println("Feature disabled")
				}
			}

		default:
			fmt.Printf("Unknown configuration key : %s", key)
		}

		fmt.Println("")
		restartDaemonIfNeeded()
	},
}

func restartDaemonIfNeeded() {
	err := exec.Command("systemctl", "--user", "is-active", "--quiet", "beamshare").Run()
	if err == nil {
		fmt.Println("Applying changes: Restarting systemd service...")
		_ = exec.Command("systemctl", "--user", "restart", "beamshare").Run()
		return
	}

	if daemon.IsRunning() {
		fmt.Println("Applying changes: Restarting background daemon...")
		execPath, err := os.Executable()
		if err == nil {
			cmd := exec.Command(execPath, "daemon", "restart")
			_ = cmd.Start()
		}
	} else {
		fmt.Println("If the daemon is running, you may need to restart it for changes to apply.")
	}
}

func init() {
	rootCmd.AddCommand(configCmd)

	configCmd.Flags().StringVarP(&setValue, "set", "s", "", "New settings")
	configCmd.Flags().IntVar(&removeIdx, "remove", 0, "Remove a device from the blacklist")
	configCmd.Flags().BoolVarP(&reset, "reset", "r", false, "Reset to default value")
	configCmd.Flags().BoolVar(&Enable, "enable", false, "Enable the feature")
	configCmd.Flags().BoolVar(&Disable, "disable", false, "Disable the feature")
	configCmd.Flags().StringVar(&Time, "time", "10:00", "Change the time")
}

func manageSystemdService(enable bool) error {
	home, err := os.UserHomeDir()
	if err != nil {
		return fmt.Errorf("failed to get user home dir: %w", err)
	}

	serviceDir := filepath.Join(home, ".config", "systemd", "user")
	servicePath := filepath.Join(serviceDir, "beamshare.service")

	if !enable {
		if _, err := os.Stat(servicePath); err == nil {
			_ = exec.Command("systemctl", "--user", "stop", "beamshare").Run()
			_ = exec.Command("systemctl", "--user", "disable", "beamshare").Run()
			_ = os.Remove(servicePath)
			_ = exec.Command("systemctl", "--user", "daemon-reload").Run()
		}
		return nil
	}

	err = os.MkdirAll(serviceDir, 0755)
	if err != nil {
		return fmt.Errorf("failed to create systemd user directory: %w", err)
	}

	execPath, err := os.Executable()
	if err != nil {
		return fmt.Errorf("failed to get executable path: %w", err)
	}

	if strings.Contains(execPath, "go-build") || strings.Contains(execPath, "Temp") {
		cwd, _ := os.Getwd()
		localBin := filepath.Join(cwd, "beamshare")
		if _, err := os.Stat(localBin); err == nil {
			execPath = localBin
		} else {
			execPath = filepath.Join(home, "go", "bin", "beamshare")
		}
	}

	serviceContent := fmt.Sprintf(`[Unit]
Description=BeamShare Daemon
After=network.target

[Service]
Type=simple
ExecStart=%s daemon run
Restart=on-failure

[Install]
WantedBy=default.target
`, execPath)

	err = os.WriteFile(servicePath, []byte(serviceContent), 0644)
	if err != nil {
		return fmt.Errorf("failed to write systemd service file: %w", err)
	}

	err = exec.Command("systemctl", "--user", "daemon-reload").Run()
	if err != nil {
		return fmt.Errorf("failed to reload systemd daemon: %w", err)
	}

	err = exec.Command("systemctl", "--user", "enable", "beamshare").Run()
	if err != nil {
		return fmt.Errorf("failed to enable systemd service: %w", err)
	}

	err = exec.Command("systemctl", "--user", "start", "beamshare").Run()
	if err != nil {
		return fmt.Errorf("failed to start systemd service: %w", err)
	}

	username := os.Getenv("USER")
	if username == "" {
		username = filepath.Base(home)
	}
	_ = exec.Command("loginctl", "enable-linger", username).Run()

	return nil
}
