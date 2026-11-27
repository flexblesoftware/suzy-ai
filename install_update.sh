#!/data/data/com.termux/files/usr/bin/bash
set -e
echo "[1/3] Applying Suzy AI v4.0 Omni-OS (150 Modules + South Hero Vikram + Chat + Voice Control)..."
git add -A
git commit -m "Upgrade to Suzy AI v4.0 Omni-OS: 150 dedicated modules, South Hero Vikram persona, full Chat tab, Owner Voice ID & live ORB/UI morphing" || true
echo "[2/3] Pushing to GitHub..."
git push
echo "[3/3] Watching GitHub Actions Cloud Build..."
sleep 6 && gh run watch
