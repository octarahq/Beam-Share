package frontend

import (
	"os"
	"os/exec"
	"strings"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/layout"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
)

type tappableText struct {
	widget.BaseWidget
	text     *canvas.Text
	OnTapped func()
}

func newTappableText(t *canvas.Text, tapped func()) *tappableText {
	w := &tappableText{text: t, OnTapped: tapped}
	w.ExtendBaseWidget(w)
	return w
}

func (w *tappableText) CreateRenderer() fyne.WidgetRenderer {
	return widget.NewSimpleRenderer(w.text)
}

func (w *tappableText) Tapped(_ *fyne.PointEvent) {
	if w.OnTapped != nil {
		w.OnTapped()
	}
}
func (w *tappableText) TappedSecondary(_ *fyne.PointEvent) {}

func Run() {
	myApp := app.New()
	myWindow := myApp.NewWindow("Beam Share")
	myWindow.Resize(fyne.NewSize(900, 600))
	hostname, err := os.Hostname()
	if err != nil {
		hostname = "Unknown Device"
	}

	var visibilityValue *widget.Button
	deviceNameValue := canvas.NewText(hostname, theme.ForegroundColor())
	deviceNameValue.TextSize = 24
	deviceNameValue.TextStyle = fyne.TextStyle{Bold: true}

	showNameDialog := func() {
		nameEntry := widget.NewEntry()
		nameEntry.SetText(hostname)

		formItems := []*widget.FormItem{
			widget.NewFormItem("New Name", nameEntry),
		}

		dialog.ShowForm("Device Name", "Save", "Cancel", formItems, func(b bool) {
			if b && nameEntry.Text != "" {
				newName := nameEntry.Text

				var cmd *exec.Cmd
				if strings.Contains(os.Args[0], "go-build") || strings.Contains(os.Args[0], "Temp") {
					cmd = exec.Command("go", "run", ".", "config", "name", "--set", newName)
				} else {
					cmd = exec.Command(os.Args[0], "config", "name", "--set", newName)
				}
				cmd.Stdout = os.Stdout
				cmd.Stderr = os.Stderr
				go cmd.Run()

				hostname = newName
				deviceNameValue.Text = newName
				deviceNameValue.Refresh()
			}
		}, myWindow)
	}

	showVisibilityDialog := func() {
		modeRadio := widget.NewRadioGroup([]string{"disabled", "trusted", "all (always)", "all (timed)"}, nil)
		modeRadio.SetSelected("all (always)")

		timeEntry := widget.NewEntry()
		timeEntry.SetText("10:00")
		timeEntry.Disable()

		modeRadio.OnChanged = func(s string) {
			if s == "all (timed)" {
				timeEntry.Enable()
			} else {
				timeEntry.Disable()
			}
		}

		formItems := []*widget.FormItem{
			widget.NewFormItem("Mode", modeRadio),
			widget.NewFormItem("Time (min:sec)", timeEntry),
		}

		dialog.ShowForm("Visibility state", "Save", "Cancel", formItems, func(b bool) {
			if b {
				mode := modeRadio.Selected
				t := timeEntry.Text

				var cmdArgs []string
				if mode == "all (timed)" {
					cmdArgs = []string{"config", "visibility", "--set", "all", "--time", t}
				} else if mode == "all (always)" {
					cmdArgs = []string{"config", "visibility", "--set", "all"}
				} else {
					cmdArgs = []string{"config", "visibility", "--set", mode}
				}

				var cmd *exec.Cmd
				if strings.Contains(os.Args[0], "go-build") || strings.Contains(os.Args[0], "Temp") {
					fullArgs := append([]string{"run", "."}, cmdArgs...)
					cmd = exec.Command("go", fullArgs...)
				} else {
					cmd = exec.Command(os.Args[0], cmdArgs...)
				}
				cmd.Stdout = os.Stdout
				cmd.Stderr = os.Stderr
				go cmd.Run()

				if visibilityValue != nil {
					displayMode := mode
					if mode == "all (always)" || mode == "all (timed)" {
						displayMode = "all"
					}
					visibilityValue.SetText("State: " + displayMode)
				}
			}
		}, myWindow)
	}

	deviceNameTitle := canvas.NewText("Device name", theme.ForegroundColor())
	deviceNameTitle.TextSize = 14

	clickableName := newTappableText(deviceNameValue, showNameDialog)

	visibilityTitle := canvas.NewText("Visibility state", theme.ForegroundColor())
	visibilityTitle.TextSize = 14

	visibilityValue = widget.NewButtonWithIcon("Always visible", theme.NavigateNextIcon(), showVisibilityDialog)
	visibilityValue.Alignment = widget.ButtonAlignLeading
	visibilityValue.Importance = widget.LowImportance

	infoText := widget.NewLabel("Nearby devices can share files with you,\nbut you'll always be notified and have to\napprove each transfer before receiving it.")
	infoText.Wrapping = fyne.TextWrapWord

	sidebar := container.NewVBox(
		deviceNameTitle,
		clickableName,
		layout.NewSpacer(),
		visibilityTitle,
		visibilityValue,
		infoText,
		layout.NewSpacer(),
		layout.NewSpacer(),
	)

	readyLabel := canvas.NewText("Ready to receive", theme.ForegroundColor())
	readyLabel.TextSize = 24
	readyLabel.TextStyle = fyne.TextStyle{Bold: true}

	receiveIcon := widget.NewIcon(theme.SearchIcon())
	centerArea := container.NewCenter(receiveIcon)

	dropIcon := widget.NewIcon(theme.UploadIcon())
	dropLabel := widget.NewLabel("Drop files to send")
	dropLabel.Alignment = fyne.TextAlignCenter
	dropLabel.TextStyle = fyne.TextStyle{Bold: true}

	selectBtn := widget.NewButtonWithIcon("Select", theme.ContentAddIcon(), func() {})
	selectBtn.Importance = widget.LowImportance

	dropZone := container.NewVBox(
		layout.NewSpacer(),
		container.NewCenter(dropIcon),
		dropLabel,
		container.NewCenter(selectBtn),
		layout.NewSpacer(),
	)

	dropCard := widget.NewCard("", "", dropZone)

	mainContent := container.NewBorder(
		container.NewPadded(readyLabel),
		container.NewPadded(dropCard),
		nil, nil,
		centerArea,
	)

	settingsBtn := widget.NewButtonWithIcon("", theme.SettingsIcon(), func() {})
	settingsBtn.Importance = widget.LowImportance
	topRight := container.NewHBox(settingsBtn)

	topBar := container.NewHBox(layout.NewSpacer(), topRight)

	paddedSidebar := container.NewPadded(sidebar)
	paddedMain := container.NewPadded(mainContent)

	split := container.NewHSplit(paddedSidebar, paddedMain)
	split.Offset = 0.35

	content := container.NewBorder(
		container.NewPadded(topBar),
		nil, nil, nil,
		split,
	)

	myWindow.SetContent(content)
	myWindow.ShowAndRun()
}
