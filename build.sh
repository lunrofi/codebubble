#!/bin/bash
# KodBalonu build script — no Gradle required.
# Requirements: JDK 17, Android SDK (platform 34, build-tools 34.0.0).
# Set KODBALONU_KS_PASS to the keystore password before running.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
: "${KODBALONU_KS_PASS:?Set KODBALONU_KS_PASS first}"
export JAVA_HOME="${JAVA_HOME:-$HOME/opt/jdk17}"
export PATH=$JAVA_HOME/bin:$PATH
SDK="${ANDROID_SDK:-$HOME/android-sdk}"
BT=$SDK/build-tools/34.0.0
PLATFORM=$SDK/platforms/android-34/android.jar
PROJ=$DIR/proj
OUT=$DIR/build
export OUT
rm -rf $OUT && mkdir -p $OUT/classes $OUT/dex $OUT/gen

$BT/aapt2 compile --dir $PROJ/res -o $OUT/res.zip
$BT/aapt2 link -o $OUT/unsigned.apk --manifest $PROJ/AndroidManifest.xml -I $PLATFORM $OUT/res.zip --auto-add-overlay --java $OUT/gen

find $PROJ/src -name "*.java" > $OUT/sources.txt
find $OUT/gen -name "*.java" >> $OUT/sources.txt
javac -encoding UTF-8 --release 8 -cp $PLATFORM -d $OUT/classes @$OUT/sources.txt

cd $OUT/classes && jar cf $OUT/app.jar . && cd $OUT
$BT/d8 --lib $PLATFORM --output $OUT/dex $OUT/app.jar

cd $OUT/dex
python3 - << 'PY'
import zipfile, glob, os
apk = os.path.join(os.environ["OUT"], "unsigned.apk")
dexes = sorted(glob.glob("classes*.dex"))
with zipfile.ZipFile(apk, "a", zipfile.ZIP_DEFLATED) as z:
    for d in dexes:
        z.write(d, d)
print("added", dexes)
PY

KS=$DIR/kodbalonu.keystore
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias kodbalonu -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$KODBALONU_KS_PASS" -keypass "$KODBALONU_KS_PASS" -dname "CN=KodBalonu, OU=KodBalonu, O=KodBalonu, C=TR"
fi

$BT/zipalign -f 4 $OUT/unsigned.apk $OUT/kodbalonu-aligned.apk
$BT/apksigner sign --ks "$KS" --ks-pass pass:"$KODBALONU_KS_PASS" --key-pass pass:"$KODBALONU_KS_PASS" \
  --out $OUT/kodbalonu.apk $OUT/kodbalonu-aligned.apk
$BT/apksigner verify $OUT/kodbalonu.apk && echo "VERIFY OK"
ls -lh $OUT/kodbalonu.apk
