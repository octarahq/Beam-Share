package daemon

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"syscall"
)

const pidFile = "/tmp/beamshare.pid"

var DebugMode bool

func Start(debug bool, foreground bool) {
	if IsRunning() {
		fmt.Println("The BeamShare daemon is already running.")
		return
	}

	DebugMode = debug

	pid := os.Getpid()
	_ = os.WriteFile(pidFile, []byte(strconv.Itoa(pid)), 0644)

	if debug || foreground {
		fmt.Printf("● BeamShare daemon started in foreground (PID: %d)...\n", pid)
		TrueRun()
	} else {
		fmt.Printf("● BeamShare daemon started in background (PID: %d).\n", pid)
		fmt.Println("Use 'daemon start --debug' or '-f' to view logs.")
		TrueRun()
	}
}

func Stop() {
	if !IsRunning() {
		fmt.Println("The BeamShare daemon is not active.")
		return
	}

	pid, _ := getPID()
	process, err := os.FindProcess(pid)
	if err != nil {
		fmt.Println("Unable to find the process:", err)
		return
	}

	err = process.Signal(syscall.SIGTERM)
	if err != nil {
		fmt.Println("Error while sending stop signal:", err)
		return
	}

	os.Remove(pidFile)
	fmt.Println("BeamShare daemon stopped.")
}

func Status() {
	if IsRunning() {
		pid, _ := getPID()
		fmt.Printf("● beamshare.service - Active\n   PID: %d\n", pid)
	} else {
		fmt.Println("○ beamshare.service - Inactive")
	}
}

func IsRunning() bool {
	pid, err := getPID()
	if err != nil {
		return false
	}

	process, err := os.FindProcess(pid)
	if err != nil {
		return false
	}

	err = process.Signal(syscall.Signal(0))
	if err == nil {
		return true
	}

	os.Remove(pidFile)
	return false
}

func getPID() (int, error) {
	data, err := os.ReadFile(pidFile)
	if err != nil {
		return 0, err
	}
	pidStr := strings.TrimSpace(string(data))
	return strconv.Atoi(pidStr)
}

func TrueRun() {
	fmt.Println("mDNS server and TCP Sockets started. Listening to network...")

	StartServer()
}
