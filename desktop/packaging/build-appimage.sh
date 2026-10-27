#!/usr/bin/env bash
# Builds Halftone-<version>-x86_64.AppImage from the Compose Desktop distributable.
# Needs: JDK 17+, curl, and FUSE (or APPIMAGE_EXTRACT_AND_RUN=1).
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION=$(grep -oP 'packageVersion = "\K[^"]+' build.gradle.kts)
ARCH=${ARCH:-x86_64}
OUT=build/appimage
APPDIR=$OUT/Halftone.AppDir

./gradlew --no-daemon createDistributable

rm -rf "$APPDIR"
mkdir -p "$APPDIR/usr/share/applications" "$APPDIR/usr/share/icons/hicolor/512x512/apps" "$APPDIR/usr/share/metainfo"
cp -a build/compose/binaries/main/app/halftone/. "$APPDIR/usr/"

cp packaging/halftone.desktop "$APPDIR/halftone.desktop"
cp packaging/halftone.desktop "$APPDIR/usr/share/applications/halftone.desktop"
cp packaging/halftone.png "$APPDIR/halftone.png"
cp packaging/halftone.png "$APPDIR/.DirIcon"
cp packaging/halftone.png "$APPDIR/usr/share/icons/hicolor/512x512/apps/halftone.png"
cp packaging/app.halftone.desktop.metainfo.xml "$APPDIR/usr/share/metainfo/"

cat > "$APPDIR/AppRun" <<'RUN'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/usr/bin/halftone" "$@"
RUN
chmod +x "$APPDIR/AppRun"

TOOL=$OUT/appimagetool
if [ ! -x "$TOOL" ]; then
  curl -fsSL -o "$TOOL" "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-$ARCH.AppImage"
  chmod +x "$TOOL"
fi

ARCH=$ARCH "$TOOL" --no-appstream "$APPDIR" "$OUT/Halftone-$VERSION-$ARCH.AppImage"
echo "Built $OUT/Halftone-$VERSION-$ARCH.AppImage"
