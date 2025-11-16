#!/usr/bin/env bash
# AI Commit CLI Installer for Linux/macOS
# Usage: curl -fsSL https://raw.githubusercontent.com/kxng0109/ai-commit-cli/main/install.sh | bash

set -e

REPO="kxng0109/ai-commit-cli"
INSTALL_DIR="${INSTALL_DIR:-/usr/local/bin}"

echo "==> Installing AI Commit CLI..."

OS=$(uname -s | tr '[:upper:]' '[:lower:]')
case "$OS" in
    linux*) PLATFORM="linux-amd64" ;;
    darwin*) PLATFORM="macos-amd64" ;;
    *) echo "Error: Unsupported OS: $OS" >&2; exit 1 ;;
esac

echo "==> Detected: $PLATFORM"

echo "==> Fetching latest version..."
VERSION=$(curl -fsSL "https://api.github.com/repos/$REPO/releases/latest" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
if [ -z "$VERSION" ]; then
    echo "Error: Failed to fetch latest version" >&2
    exit 1
fi

echo "==> Latest version: $VERSION"

URL="https://github.com/$REPO/releases/download/$VERSION/ai-commit-$PLATFORM"
TMP="/tmp/ai-commit-$"

echo "==> Downloading..."
if command -v curl &> /dev/null; then
    curl -fsSL "$URL" -o "$TMP"
elif command -v wget &> /dev/null; then
    wget -q "$URL" -O "$TMP"
else
    echo "Error: Neither curl nor wget found" >&2
    exit 1
fi

chmod +x "$TMP"

echo "==> Installing to $INSTALL_DIR..."
if [ -w "$INSTALL_DIR" ]; then
    mv "$TMP" "$INSTALL_DIR/ai-commit"
else
    echo "    (requires sudo)"
    sudo mv "$TMP" "$INSTALL_DIR/ai-commit"
fi

if command -v ai-commit &> /dev/null; then
    INSTALLED_VERSION=$(ai-commit --version 2>&1 || echo "unknown")
    echo "==> ✓ Installation complete: $INSTALLED_VERSION"
else
    echo "==> ✓ Installed to $INSTALL_DIR/ai-commit"
    echo "    Note: You may need to restart your terminal"
fi

echo ""
echo "Next steps:"
echo "  1. Configure: export OPENAI_API_KEY=\"sk-...\""
echo "  2. Use: cd your-repo && git add . && ai-commit"
echo ""
echo "Run 'ai-commit --help' for more info"