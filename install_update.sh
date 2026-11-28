#!/data/data/com.termux/files/usr/bin/bash
set -e
echo "[1/4] Patching AndroidManifest.xml with Accessibility Commander & Special Permissions..."
python3 patch_manifest_and_push.py
echo "[2/4] Staging all 174 Omni-OS modules..."
git add -A
git commit -m "Suzy AI v4.2 Omni-OS: Permission Manager, Accessibility Commander, Real Voice/Hardware Commands & Voiceprint DSP" || true
echo "[3/4] Pushing to GitHub..."
git push
echo "[4/4] Watching GitHub Actions Cloud Build..."
sleep 6 && gh run watch
