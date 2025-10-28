#!/bin/bash
set -e

echo "🔍 Starting APK Validation..."

APK_PATH="android-fdroid/app/build/outputs/apk/debug/app-debug.apk"

# 1. Check APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH"
    exit 1
fi
echo "✅ APK found"

# 2. Check APK is valid zip
if ! unzip -t "$APK_PATH" > /dev/null 2>&1; then
    echo "❌ APK is corrupt (invalid zip)"
    exit 1
fi
echo "✅ APK zip structure valid"

# 3. Check critical files exist
REQUIRED_FILES=("AndroidManifest.xml" "classes.dex" "resources.arsc")
for file in "${REQUIRED_FILES[@]}"; do
    if ! unzip -l "$APK_PATH" | grep -q "$file"; then
        echo "❌ Missing critical file: $file"
        exit 1
    fi
done
echo "✅ All critical APK files present"

# 4. Validate AndroidManifest
if ! aapt dump badging "$APK_PATH" > /dev/null 2>&1; then
    echo "❌ Invalid AndroidManifest.xml"
    exit 1
fi
echo "✅ AndroidManifest valid"

# 5. Check IME components
if ! aapt dump xmltree "$APK_PATH" AndroidManifest.xml | grep -q "BIND_INPUT_METHOD"; then
    echo "❌ IME service missing in manifest"
    exit 1
fi
echo "✅ IME service found"

# 6. Check method.xml
if ! aapt list "$APK_PATH" | grep -q "xml/method.xml"; then
    echo "❌ method.xml missing"
    exit 1
fi
echo "✅ method.xml found"

# 7. Check DEX has classes
DEX_COUNT=$(aapt list "$APK_PATH" | grep -c ".dex")
if [ "$DEX_COUNT" -eq 0 ]; then
    echo "❌ No DEX files found"
    exit 1
fi
echo "✅ DEX files found: $DEX_COUNT"

# 8. Check resources
RES_COUNT=$(aapt list "$APK_PATH" | grep -c "res/")
if [ "$RES_COUNT" -lt 10 ]; then
    echo "❌ Too few resources: $RES_COUNT"
    exit 1
fi
echo "✅ Resources found: $RES_COUNT"

# 9. Check package name
PACKAGE=$(aapt dump badging "$APK_PATH" | grep "package: name" | cut -d"'" -f2)
if [ -z "$PACKAGE" ]; then
    echo "❌ No package name found"
    exit 1
fi
echo "✅ Package name: $PACKAGE"

echo "🎉 APK validation passed! Package should install correctly."
