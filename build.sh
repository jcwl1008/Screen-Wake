#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK with platform 35 and build-tools 35.0.0}"
BUILD_TOOLS="$ANDROID_HOME/build-tools/35.0.0"
ANDROID_JAR="$ANDROID_HOME/platforms/android-35/android.jar"
if [ ! -f "$ANDROID_JAR" ]; then ANDROID_JAR="$ANDROID_HOME/platforms/android-35/android-35/android.jar"; fi
mkdir -p build/classes build/dex signing
"$BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$BUILD_TOOLS/aapt2" link -o build/resources.apk -I "$ANDROID_JAR" --manifest app/src/main/AndroidManifest.xml --java build/generated build/resources.zip
find app/src/main/java build/generated -name '*.java' > build/sources.txt
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$ANDROID_JAR:$BUILD_TOOLS/core-lambda-stubs.jar" -d build/classes @build/sources.txt
find build/classes -name '*.class' > build/classes.txt
"$BUILD_TOOLS/d8" --min-api 28 --lib "$ANDROID_JAR" --output build/dex @build/classes.txt
cp build/resources.apk build/unsigned.apk
(cd build/dex && zip -q -u ../unsigned.apk classes*.dex)
"$BUILD_TOOLS/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [ ! -f signing/screenwake.p12 ]; then
    python3 -c 'import secrets; print(secrets.token_urlsafe(32))' > signing/password.txt
    keytool -genkeypair -keystore signing/screenwake.p12 -storetype PKCS12 -alias screenwake -storepass:file signing/password.txt -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Screen Wake, OU=Showroom, O=Jimmy, C=HK'
fi
"$BUILD_TOOLS/apksigner" sign --ks signing/screenwake.p12 --ks-key-alias screenwake --ks-pass file:signing/password.txt --out build/ScreenWake.apk build/aligned.apk
"$BUILD_TOOLS/apksigner" verify --verbose build/ScreenWake.apk
echo 'Built build/ScreenWake.apk'
