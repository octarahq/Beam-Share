package frontend

import (
	"beam-share-cli/internal/crypto"
	"encoding/json"
	"fmt"
	"image/color"
	_ "image/jpeg"
	_ "image/png"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/layout"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
)


type tappableContainer struct {
	widget.BaseWidget
	content  fyne.CanvasObject
	OnTapped func()
}

func newTappableContainer(c fyne.CanvasObject, tapped func()) *tappableContainer {
	t := &tappableContainer{content: c, OnTapped: tapped}
	t.ExtendBaseWidget(t)
	return t
}

func (t *tappableContainer) CreateRenderer() fyne.WidgetRenderer {
	return widget.NewSimpleRenderer(t.content)
}

func (t *tappableContainer) Tapped(_ *fyne.PointEvent) {
	if t.OnTapped != nil {
		t.OnTapped()
	}
}

func (t *tappableContainer) TappedSecondary(_ *fyne.PointEvent) {}

type tappableText struct {
	widget.BaseWidget
	text     *canvas.Text
	OnTapped func()
}



type DeviceInfo struct {
	Name   string `json:"name"`
	Ip     string `json:"ip"`
	Port   int    `json:"port"`
	NodeId string `json:"node_id"`
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
	myApp := app.NewWithID("com.octarahq.beamshare")
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

		var dropZone *fyne.Container
	var dropCard *widget.Card
	var selectedFile string

	var selectBtn *widget.Button
	
	var deviceMu sync.Mutex
	deviceStates := make(map[string]string)
	deviceTimes := make(map[string]time.Duration)
	pollingActive := false

	showDevicesUI := func() {
		deviceMu.Lock()
		deviceStates = make(map[string]string)
		deviceTimes = make(map[string]time.Duration)
		deviceMu.Unlock()

		pollingActive = true
		loadingLabel := widget.NewLabel("Searching for nearby devices...")
		
		scrollBox := container.NewHBox()
		devicesScroll := container.NewHScroll(scrollBox)
		devicesScroll.SetMinSize(fyne.NewSize(0, 180))

		content := container.NewVBox(
			widget.NewLabelWithStyle("Select a recipient", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
			loadingLabel,
			devicesScroll,
			widget.NewLabel("Ready to send:\n" + filepath.Base(selectedFile)),
			widget.NewButton("Cancel", func() {
				pollingActive = false
				selectedFile = ""
				dropCard.SetContent(container.NewPadded(dropZone))
			}),
		)

		dropCard.SetContent(container.NewPadded(content))

		go func() {
			for pollingActive {
				conn, err := net.Dial("unix", "/tmp/beamshare.sock")
				if err != nil {
					time.Sleep(2 * time.Second)
					continue
				}

				encoder := json.NewEncoder(conn)
				decoder := json.NewDecoder(conn)

				_ = encoder.Encode(MessageIPC{Event: "list_devices"})
				var devices []DeviceInfo
				_ = decoder.Decode(&devices)
				conn.Close()

				var newObjects []fyne.CanvasObject

				myNodeId, _ := crypto.GetNodeID()

				for _, d := range devices {
					nodeId := d.NodeId
					if nodeId == myNodeId {
						continue
					}
					devIp := d.Ip
					
					deviceMu.Lock()
					state := deviceStates[nodeId]
					deviceMu.Unlock()
					
					if state == "" {
						state = "ready"
					}

					initials := ""
					if len(d.Name) >= 2 {
						initials = strings.ToUpper(d.Name[:2])
					} else if len(d.Name) == 1 {
						initials = strings.ToUpper(d.Name)
					}

					circleColor := color.NRGBA{R: 50, G: 150, B: 50, A: 255}
					if state == "done" {
						circleColor = color.NRGBA{R: 30, G: 120, B: 30, A: 255}
					} else if state == "error" {
						circleColor = color.NRGBA{R: 200, G: 50, B: 50, A: 255}
					}

					circle := canvas.NewCircle(circleColor)
					sizedCircle := container.NewGridWrap(fyne.NewSize(80, 80), circle)

					var centerIcon fyne.CanvasObject
					if state == "done" {
						icon := canvas.NewImageFromResource(theme.ConfirmIcon())
						centerIcon = container.NewGridWrap(fyne.NewSize(40, 40), icon)
					} else {
						text := canvas.NewText(initials, color.White)
						text.TextSize = 28
						text.Alignment = fyne.TextAlignCenter
						centerIcon = text
					}

					statusText := "Ready to receive"
					if state == "sending" {
						statusText = "Transferring..."
					} else if state == "done" {
						statusText = fmt.Sprintf("Transfer finished (%dms)", deviceTimes[nodeId].Milliseconds())
					} else if state == "error" {
						statusText = "Transfer failed"
					}

					subLabel := widget.NewLabelWithStyle(statusText, fyne.TextAlignCenter, fyne.TextStyle{})
					if state == "done" {
						subLabel.TextStyle = fyne.TextStyle{Bold: true}
					}

					var topArea fyne.CanvasObject = container.NewCenter(container.NewStack(sizedCircle, centerIcon))
					if state == "sending" {
						prog := widget.NewProgressBarInfinite()
						topArea = container.NewVBox(
							container.NewCenter(container.NewStack(sizedCircle, centerIcon)),
							prog,
						)
					}

					cardStack := container.NewVBox(
						topArea,
						widget.NewLabelWithStyle(d.Name, fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
						subLabel,
					)
					
					tappableCard := newTappableContainer(cardStack, func() {
						deviceMu.Lock()
						if deviceStates[nodeId] == "sending" || deviceStates[nodeId] == "done" {
							deviceMu.Unlock()
							return
						}
						deviceStates[nodeId] = "sending"
						deviceMu.Unlock()
						
						go func() {
							start := time.Now()
							var cmd *exec.Cmd
							if strings.Contains(os.Args[0], "go-build") || strings.Contains(os.Args[0], "Temp") {
								cmd = exec.Command("go", "run", ".", "send", "--to", devIp, "--port", strconv.Itoa(d.Port), selectedFile)
							} else {
								cmd = exec.Command(os.Args[0], "send", "--to", devIp, "--port", strconv.Itoa(d.Port), selectedFile)
							}
							out, err := cmd.CombinedOutput()
							fmt.Println("Send output:", string(out))
							outStr := string(out)
							dur := time.Since(start)
							if strings.Contains(outStr, "successfully! (") {
								parts := strings.Split(outStr, "successfully! (")
								if len(parts) > 1 {
									timeStr := strings.Split(parts[1], "ms)")[0]
									parsedMs, _ := time.ParseDuration(timeStr + "ms")
									dur = parsedMs
								}
							}
							
							deviceMu.Lock()
							if err == nil {
								deviceStates[nodeId] = "done"
							} else {
								deviceStates[nodeId] = "error"
							}
							deviceTimes[nodeId] = dur
							deviceMu.Unlock()
						}()
					})
					
					newObjects = append(newObjects, container.NewPadded(tappableCard))
				}
				
				devCount := len(devices)

				fyne.Do(func() {
					if devCount > 0 {
						loadingLabel.SetText(fmt.Sprintf("Found %d devices", devCount))
					} else {
						loadingLabel.SetText("Searching for nearby devices...")
					}
					scrollBox.Objects = newObjects
					scrollBox.Refresh()
				})

				time.Sleep(2 * time.Second)
			}
		}()
	}

	selectBtn = widget.NewButtonWithIcon("Select file...", theme.ContentAddIcon(), func() {
		dialog.ShowFileOpen(func(uc fyne.URIReadCloser, err error) {
			if err != nil || uc == nil {
				return
			}
			selectedFile = uc.URI().Path()
			showDevicesUI()
		}, myWindow)
	})
	selectBtn.Importance = widget.HighImportance

	dropZone = container.NewVBox(
		container.NewCenter(dropIcon),
		widget.NewLabel(""),
		container.NewCenter(dropLabel),
		widget.NewLabel(""),
		container.NewCenter(selectBtn),
	)

	dropCard = widget.NewCard("", "", container.NewPadded(dropZone))

	myWindow.SetOnDropped(func(pos fyne.Position, uris []fyne.URI) {
		if len(uris) > 0 {
			selectedFile = uris[0].Path()
			showDevicesUI()
		}
	})


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
