#!/bin/bash
# Genera el proyecto de Xcode. Si GoogleService-Info.plist trae REVERSED_CLIENT_ID (cliente OAuth
# de iOS creado en Firebase), lo agrega como esquema de URL para que Google pueda volver a la app.
set -euo pipefail
cd "$(dirname "$0")/../iosApp"
PLIST=iosApp/GoogleService-Info.plist
REV=$(/usr/libexec/PlistBuddy -c "Print :REVERSED_CLIENT_ID" "$PLIST" 2>/dev/null || true)
if [ -n "$REV" ]; then
  echo "Esquema de Google Sign-In: $REV"
  python3 - "$REV" <<'PY'
import sys
rev=sys.argv[1]; p='project.yml'; s=open(p).read()
marker='          # REVERSED_CLIENT_ID de Google Sign-In: lo agrega scripts/generar_proyecto.sh si el plist lo trae'
if rev not in s:
    s=s.replace(marker, marker+'\n          - CFBundleURLName: google-sign-in\n            CFBundleURLSchemes: ['+rev+']')
open(p,'w').write(s)
PY
else
  echo "GoogleService-Info.plist sin REVERSED_CLIENT_ID: se usará el inicio de sesión web de Firebase."
fi
xcodegen generate
