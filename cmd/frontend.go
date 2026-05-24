package cmd

import (
	"github.com/spf13/cobra"
	"beam-share-cli/frontend"
)

var frontendCmd = &cobra.Command{
	Use:   "frontend",
	Short: "Démarrer l'interface graphique",
	Long:  `Démarrer l'interface graphique de Beam Share développée avec Fyne.`,
	Run: func(cmd *cobra.Command, args []string) {
		frontend.Run()
	},
}

func init() {
	rootCmd.AddCommand(frontendCmd)
}
