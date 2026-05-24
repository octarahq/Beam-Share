package frontend

import (
	"image/color"
	_ "image/jpeg"
	_ "image/png"
	"os"
	"os/exec"
	"runtime"
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

func openFile(path string) {
	if runtime.GOOS == "windows" {
		exec.Command("cmd", "/c", "start", path).Start()
	} else if runtime.GOOS == "darwin" {
		exec.Command("open", path).Start()
	} else {
		exec.Command("xdg-open", path).Start()
	}
}

func openFolder(path string) {
	openFile(path)
}

type PendingTransfer struct {
	ID       string `json:"id"`
	FileName string `json:"file_name"`
	Size     int64  `json:"size"`
	Sender   string `json:"sender"`
	SenderID string `json:"sender_id"`
	Status   string `json:"status"`
}

type MessageIPC struct {
	Event  string `json:"event"`
	ID     string `json:"id"`
	File   string `json:"file"`
	Size   int64  `json:"size"`
	From   string `json:"from"`
	Value  string `json:"value"`
	NodeId string `json:"node_id"`
}

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

		d := dialog.NewCustomConfirm("Device Name", "Save", "Cancel", nameEntry, func(b bool) {
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
		d.Resize(fyne.NewSize(350, 150))
		d.Show()
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

	importColor := color.NRGBA{R: 140, G: 140, B: 140, A: 255}
	deviceNameTitle := canvas.NewText("Device name", importColor)
	deviceNameTitle.TextSize = 13

	clickableName := newTappableText(deviceNameValue, showNameDialog)

	visibilityTitle := canvas.NewText("Visibility state", importColor)
	visibilityTitle.TextSize = 13

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
	readyLabel.TextSize = 28
	readyLabel.TextStyle = fyne.TextStyle{Bold: true}

	compIcon := canvas.NewImageFromResource(theme.ComputerIcon())
	compIcon.SetMinSize(fyne.NewSize(64, 64))
	compIcon.FillMode = canvas.ImageFillContain

	centerArea := container.NewVBox(
		layout.NewSpacer(),
		container.NewCenter(compIcon),
		widget.NewLabel(""),
		layout.NewSpacer(),
	)

	dropIcon := canvas.NewImageFromResource(theme.UploadIcon())
	dropIcon.SetMinSize(fyne.NewSize(48, 48))
	dropIcon.FillMode = canvas.ImageFillContain

	dropLabel := canvas.NewText("Drop files to send", theme.ForegroundColor())
	dropLabel.TextSize = 18
	dropLabel.TextStyle = fyne.TextStyle{Bold: true}

	selectBtn := widget.NewButtonWithIcon("Select file...", theme.ContentAddIcon(), func() {})
	selectBtn.Importance = widget.HighImportance

	dropZone := container.NewVBox(
		container.NewCenter(dropIcon),
		widget.NewLabel(""),
		container.NewCenter(dropLabel),
		widget.NewLabel(""),
		container.NewCenter(selectBtn),
	)

	dropCard := widget.NewCard("", "", container.NewPadded(dropZone))

	headerBar := container.NewHBox(readyLabel, layout.NewSpacer())

	mainContent := container.NewBorder(
		container.NewPadded(headerBar),
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
