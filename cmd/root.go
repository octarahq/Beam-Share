package cmd

import (
	"os"

	"github.com/spf13/cobra"
)

var rootCmd = &cobra.Command{
	Use:   "beam-share-cli",
	Short: "Root of Beam Share CLI",
	Long: `Beam Share CLI, permet de transmettre des fichiers en utilisant le protocole Beam Share.
	
	Il permet d'envoyer, recevoir des fichiers.`,
	Run: func(cmd *cobra.Command, args []string) {
		cmd.Help()
	},
}

func Execute() {
	err := rootCmd.Execute()
	if err != nil {
		os.Exit(1)
	}
}

func init() {

}
