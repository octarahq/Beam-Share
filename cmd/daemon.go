package cmd

import (
	"beam-share-cli/internal/daemon"

	"github.com/spf13/cobra"
)

var isDebug bool
var isForeground bool

var daemonCmd = &cobra.Command{
	Use:   "daemon",
	Short: "Manage the BeamShare daemon in the background",
	Long:  "To start the daemon in background run `daemon start &`",
}

var startCmd = &cobra.Command{
	Use:   "start",
	Short: "Start the daemon in the background",
	Run: func(cmd *cobra.Command, args []string) {
		daemon.Start(isDebug, isForeground)
	},
}

var stopCmd = &cobra.Command{
	Use:   "stop",
	Short: "Stop the daemon in the background",
	Run: func(cmd *cobra.Command, args []string) {
		daemon.Stop()
	},
}

var statusCmd = &cobra.Command{
	Use:   "status",
	Short: "View daemon status",
	Run: func(cmd *cobra.Command, args []string) {
		daemon.Status()
	},
}

var restartCmd = &cobra.Command{
	Use:   "restart",
	Short: "Restart the daemon",
	Run: func(cmd *cobra.Command, args []string) {
		daemon.Stop()
		daemon.Start(isDebug, isForeground)
	},
}

var runCmd = &cobra.Command{
	Use:    "run",
	Short:  "Run the server (Blocking, used by the system)",
	Hidden: true,
	Run: func(cmd *cobra.Command, args []string) {
		daemon.TrueRun()
	},
}

func init() {
	rootCmd.AddCommand(daemonCmd)

	startCmd.Flags().BoolVarP(&isDebug, "debug", "d", false, "Enable logs")
	startCmd.Flags().BoolVarP(&isForeground, "foreground", "f", false, "Start at foreground")

	daemonCmd.AddCommand(startCmd)
	daemonCmd.AddCommand(stopCmd)
	daemonCmd.AddCommand(statusCmd)
	daemonCmd.AddCommand(restartCmd)
	daemonCmd.AddCommand(runCmd)
}
