#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status
set -e

REPO="octarahq/Beam-Share"
API_URL="https://api.github.com/repos/${REPO}/releases"

echo "=========================================="
echo "      BeamShare Installer for Linux       "
echo "=========================================="

# Check if run as root or sudo
if [ "$EUID" -eq 0 ]; then
    echo "Error: Please DO NOT run this script as root or with sudo." >&2
    echo "This installer is designed to install BeamShare locally for your user account." >&2
    echo "Please run it as a normal user: ./install.sh" >&2
    exit 1
fi

# Check dependencies
for cmd in curl grep; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "Error: '$cmd' is required but not installed. Please install it and try again." >&2
        exit 1
    fi
done

echo "Fetching releases from GitHub..."
releases_json=$(curl -sL -H "User-Agent: BeamShare-Installer" "$API_URL")

if [ -z "$releases_json" ] || echo "$releases_json" | grep -q '"message":'; then
    echo "Error: Failed to fetch releases from GitHub API. Please check your connection or try again later." >&2
    if echo "$releases_json" | grep -q 'rate limit'; then
        echo "Reason: GitHub API rate limit exceeded." >&2
    fi
    exit 1
fi

DOWNLOAD_URL=""

# 1. Try parsing with python3 (extremely robust)
if [ -z "$DOWNLOAD_URL" ] && command -v python3 >/dev/null 2>&1; then
    DOWNLOAD_URL=$(echo "$releases_json" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if not isinstance(data, list):
        sys.exit(1)
    for release in data:
        for asset in release.get('assets', []):
            if asset['name'] == 'beamshare':
                print(asset['browser_download_url'])
                sys.exit(0)
    sys.exit(1)
except Exception:
    sys.exit(2)
" 2>/dev/null || true)
fi

# 2. Try parsing with jq if python3 wasn't successful/available
if [ -z "$DOWNLOAD_URL" ] && command -v jq >/dev/null 2>&1; then
    DOWNLOAD_URL=$(echo "$releases_json" | jq -r 'map(select(.assets != null) | .assets[] | select(.name == "beamshare"))[0].browser_download_url' 2>/dev/null || true)
    if [ "$DOWNLOAD_URL" = "null" ]; then
        DOWNLOAD_URL=""
    fi
fi

# 3. Fallback to grep/sed parsing if others fail
if [ -z "$DOWNLOAD_URL" ] && [ -n "$(echo "$releases_json" | grep -oP '"browser_download_url":\s*"\K[^"]+' | grep '/releases/download/[^/]*/beamshare$')" ]; then
    DOWNLOAD_URL=$(echo "$releases_json" | grep -oP '"browser_download_url":\s*"\K[^"]+' | grep '/releases/download/[^/]*/beamshare$' | head -n 1 || true)
fi

# Verify we got a download URL
if [ -z "$DOWNLOAD_URL" ]; then
    echo "Error: Could not find any release containing the 'beamshare' binary." >&2
    exit 1
fi

echo "Found binary at: $DOWNLOAD_URL"

# Define install directories (user-level to avoid root requirement)
BIN_DIR="$HOME/.local/bin"
APP_DIR="$HOME/.local/share/applications"
ICON_DIR="$HOME/.local/share/beamshare"

echo "Creating installation directories..."
mkdir -p "$BIN_DIR"
mkdir -p "$APP_DIR"
mkdir -p "$ICON_DIR"

# Stop existing service first if it is running to avoid conflict
if systemctl --user is-active --quiet beamshare 2>/dev/null; then
    echo "Stopping currently running BeamShare service..."
    systemctl --user stop beamshare || true
fi

# Remove old binary if it exists to avoid "text file busy" when overwriting
rm -f "$BIN_DIR/beamshare"

# Download the binary
echo "Downloading BeamShare binary..."
curl -L -o "$BIN_DIR/beamshare" "$DOWNLOAD_URL"
chmod +x "$BIN_DIR/beamshare"

# Copy or download the icon
if [ -f "./assets/favicon.png" ]; then
    echo "Copying icon from local workspace..."
    cp "./assets/favicon.png" "$ICON_DIR/favicon.png"
else
    echo "Downloading icon from GitHub..."
    curl -sL -o "$ICON_DIR/favicon.png" "https://raw.githubusercontent.com/octarahq/Beam-Share/linux-cli/assets/favicon.png"
fi

# Create the desktop entry
echo "Creating application shortcut..."
cat <<EOF > "$APP_DIR/beamshare.desktop"
[Desktop Entry]
Type=Application
Name=BeamShare
Comment=Share files over the local network
Exec=$BIN_DIR/beamshare frontend
Icon=$ICON_DIR/favicon.png
Terminal=false
Categories=Network;FileTransfer;Utility;
EOF

chmod +x "$APP_DIR/beamshare.desktop"

# Refresh desktop database if tool exists
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$APP_DIR"
fi

# Enable startup on boot (writes systemd service and starts the daemon)
echo "Enabling startup on boot and starting the service..."
"$BIN_DIR/beamshare" config startonboot --set true

echo "=========================================="
echo "Installation complete!"
echo "BeamShare is installed in: $BIN_DIR/beamshare"
echo "You can launch the GUI from your application menu."
echo "=========================================="
