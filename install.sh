#!/usr/bin/env bash
# AI Commit CLI Installer for Linux/macOS
# Usage: curl -fsSL https://raw.githubusercontent.com/kxng0109/ai-commit-cli/main/install.sh | bash
# Pinned version: AI_COMMIT_VERSION=v1.2.0 bash install.sh
# Verify only (no install): bash install.sh --verify-only [--version=v1.2.0]

set -euo pipefail

REPO="kxng0109/ai-commit-cli"
INSTALL_DIR="${INSTALL_DIR:-/usr/local/bin}"
PINNED_VERSION="${AI_COMMIT_VERSION:-}"
VERIFY_ONLY=0

usage() {
    echo "Usage: install.sh [--verify-only] [--version=vX.Y.Z]"
    echo "  --verify-only   Download and verify the checksum without installing"
    echo "  --version=...   Install a pinned version instead of the latest release"
    echo "Env: AI_COMMIT_VERSION pins the version, INSTALL_DIR overrides the target directory"
}

for arg in "$@"; do
    case "$arg" in
        --verify-only) VERIFY_ONLY=1 ;;
        --version=*) PINNED_VERSION="${arg#--version=}" ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Error: Unknown option: $arg" >&2; usage >&2; exit 1 ;;
    esac
done

echo "==> Installing AI Commit CLI..."

OS=$(uname -s | tr '[:upper:]' '[:lower:]')
case "$OS" in
    linux*) PLATFORM="linux-amd64" ;;
    darwin*) PLATFORM="macos-amd64" ;;
    *) echo "Error: Unsupported OS: $OS" >&2; exit 1 ;;
esac

echo "==> Detected: $PLATFORM"

if [ -n "$PINNED_VERSION" ]; then
    VERSION="$PINNED_VERSION"
else
    echo "==> Fetching latest version..."
    VERSION=$(curl -fsSL "https://api.github.com/repos/$REPO/releases/latest" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
fi
if [[ ! "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: Refusing to use unexpected version value: ${VERSION:-<empty>}" >&2
    exit 1
fi

echo "==> Version: $VERSION"

ASSET="ai-commit-$PLATFORM"
URL="https://github.com/$REPO/releases/download/$VERSION/$ASSET"
SHA_URL="$URL.sha256"

TMPDIR=$(mktemp -d -t ai-commit-install.XXXXXX)
cleanup() {
    rm -rf "$TMPDIR"
}
trap cleanup EXIT

echo "==> Downloading..."
if command -v curl &> /dev/null; then
    curl -fsSL "$URL" -o "$TMPDIR/$ASSET"
    curl -fsSL "$SHA_URL" -o "$TMPDIR/$ASSET.sha256"
elif command -v wget &> /dev/null; then
    wget -q "$URL" -O "$TMPDIR/$ASSET"
    wget -q "$SHA_URL" -O "$TMPDIR/$ASSET.sha256"
else
    echo "Error: Neither curl nor wget found" >&2
    exit 1
fi

echo "==> Verifying checksum..."
EXPECTED=$(awk '{print $1}' "$TMPDIR/$ASSET.sha256")
if [ -z "$EXPECTED" ]; then
    echo "Error: Empty checksum file" >&2
    exit 1
fi
if command -v sha256sum &> /dev/null; then
    ACTUAL=$(sha256sum "$TMPDIR/$ASSET" | awk '{print $1}')
elif command -v shasum &> /dev/null; then
    ACTUAL=$(shasum -a 256 "$TMPDIR/$ASSET" | awk '{print $1}')
else
    echo "Error: Neither sha256sum nor shasum found" >&2
    exit 1
fi
if [ "$ACTUAL" != "$EXPECTED" ]; then
    echo "Error: Checksum mismatch for $ASSET (download may be tampered)" >&2
    exit 1
fi
echo "==> Checksum OK"

if [ "$VERIFY_ONLY" -eq 1 ]; then
    echo "==> Verify-only mode: checksum valid, nothing installed"
    exit 0
fi

chmod 755 "$TMPDIR/$ASSET"

echo "==> Installing to $INSTALL_DIR..."
if [ -w "$INSTALL_DIR" ]; then
    mv "$TMPDIR/$ASSET" "$INSTALL_DIR/ai-commit"
else
    echo "    (requires sudo)"
    sudo mv "$TMPDIR/$ASSET" "$INSTALL_DIR/ai-commit"
fi

if command -v ai-commit &> /dev/null; then
    INSTALLED_VERSION=$(ai-commit --version 2>&1 || echo "unknown")
    echo "==> Installation complete: $INSTALLED_VERSION"
else
    echo "==> Installed to $INSTALL_DIR/ai-commit"
    echo "    Note: You may need to restart your terminal"
fi

echo ""
echo "Next steps:"
echo "  1. Configure: export OPENAI_API_KEY=\"sk-...\""
echo "  2. Use: cd your-repo && git add . && ai-commit"
echo ""
echo "Run 'ai-commit --help' for more info"
