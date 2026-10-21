#!/usr/bin/env bash
# SkyWar APK 构建脚本（不依赖 Gradle：aapt2 + javac + d8 + zipalign + apksigner）
set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLCHAIN="C:/Users/wzw/.workbuddy/toolchain"
SDK="$TOOLCHAIN/android-sdk"
BT="$SDK/build-tools/36.1.0"
PLATFORM="$SDK/platforms/android-36/android.jar"
JAVA_HOME="$TOOLCHAIN/jdk17"
JAVA="$JAVA_HOME/bin/java.exe"
JAVAC="$JAVA_HOME/bin/javac.exe"
KEYTOOL="$JAVA_HOME/bin/keytool.exe"
SIGN_DIR="$TOOLCHAIN/skywar-signing"
KEYSTORE="$SIGN_DIR/skywar-release.jks"

cd "$ROOT"

VERSION_NAME=$(grep -o '[0-9]*\.[0-9]*' VERSION | head -1)
VERSION_CODE=$(grep -o 'versionCode=[0-9]*' VERSION | grep -o '[0-9]*')
echo "== SkyWar build: versionName=$VERSION_NAME versionCode=$VERSION_CODE =="

rm -rf build
mkdir -p build/gen build/classes build/dex build/apk dist

echo "-- aapt2 compile resources"
"$BT/aapt2.exe" compile --dir res -o build/res.zip

echo "-- aapt2 link"
"$BT/aapt2.exe" link \
  -o build/apk/base.apk \
  -I "$PLATFORM" \
  --manifest AndroidManifest.xml \
  -R build/res.zip \
  --java build/gen \
  --min-sdk-version 21 \
  --target-sdk-version 34 \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  --auto-add-overlay

echo "-- javac"
find src build/gen -name "*.java" > build/sources.txt
"$JAVAC" -nowarn -encoding UTF-8 -source 8 -target 8 \
  -bootclasspath "$PLATFORM" -classpath "$PLATFORM" \
  -d build/classes @build/sources.txt 2>&1 | grep -v "bootstrap class path" | grep -v "^Note:" || true
if [ ! -f build/classes/com/zhiwei/skywar/GameActivity.class ]; then
  echo "!! javac failed"; exit 1
fi

echo "-- d8 dex"
"$JAVA" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 \
  --release --lib "$PLATFORM" --min-api 21 \
  --output build/dex \
  $(find build/classes -name "*.class")

echo "-- package dex into apk"
"C:/Users/wzw/.workbuddy/binaries/python/versions/3.13.12/python.exe" tools/inject_dex.py build/apk/base.apk build/dex/classes.dex build/apk/withdex.apk

echo "-- zipalign"
"$BT/zipalign.exe" -f -p 4 build/apk/withdex.apk build/apk/aligned.apk

echo "-- sign"
if [ ! -f "$KEYSTORE" ]; then
  mkdir -p "$SIGN_DIR"
  "$KEYTOOL" -genkeypair -keystore "$KEYSTORE" -alias skywar \
    -keyalg RSA -keysize 2048 -validity 12000 \
    -storepass SkyWar2026Sign -keypass SkyWar2026Sign \
    -dname "CN=SkyWar, OU=Game, O=ZhiWei, L=Shenzhen, ST=Guangdong, C=CN"
fi

"$JAVA" -jar "$BT/lib/apksigner.jar" sign \
  --ks "$KEYSTORE" --ks-key-alias skywar \
  --ks-pass pass:SkyWar2026Sign --key-pass pass:SkyWar2026Sign \
  --min-sdk-version 21 --v1-signing-enabled true --v2-signing-enabled true \
  --out "dist/SkyWar-$VERSION_NAME.apk" build/apk/aligned.apk

"$JAVA" -jar "$BT/lib/apksigner.jar" verify --print-certs "dist/SkyWar-$VERSION_NAME.apk" | head -3
cp "dist/SkyWar-$VERSION_NAME.apk" dist/SkyWar.apk

SIZE=$(du -h "dist/SkyWar-$VERSION_NAME.apk" | cut -f1)
echo "== BUILD OK == dist/SkyWar.apk ($SIZE)"
