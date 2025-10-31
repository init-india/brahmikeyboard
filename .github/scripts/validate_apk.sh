#!/bin/bash
echo "🔍 Brahmi Keyboard APK Validation"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

validate_apk() {
    local apk_path=$1
    local apk_name=$(basename "$apk_path")
    
    echo -e "\n📱 Validating: $apk_name"
    
    if [ ! -f "$apk_path" ]; then
        echo -e "${RED}❌ APK not found: $apk_path${NC}"
        return 1
    fi
    
    # Check if aapt is available
    if ! command -v aapt &> /dev/null; then
        echo -e "${YELLOW}⚠️  aapt not available, skipping deep validation${NC}"
        echo -e "${GREEN}✅ APK exists: $(ls -lh "$apk_path")${NC}"
        return 0
    fi
    
    # Basic APK validation
    if aapt dump badging "$apk_path" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ APK is valid${NC}"
        
        # Extract package info
        local package=$(aapt dump badging "$apk_path" | grep "package: name=" | sed "s/.*name='\([^']*\)'.*/\1/")
        echo "📦 Package: $package"
        
        # Check for IME service
        if aapt dump xmltree "$apk_path" AndroidManifest.xml | grep -q "InputMethod"; then
            echo -e "${GREEN}✅ IME service declared${NC}"
        else
            echo -e "${RED}❌ No IME service found${NC}"
        fi
        
        # Check permissions
        if aapt dump permissions "$apk_path" | grep -q "BIND_INPUT_METHOD"; then
            echo -e "${GREEN}✅ BIND_INPUT_METHOD permission found${NC}"
        else
            echo -e "${RED}❌ Missing BIND_INPUT_METHOD permission${NC}"
        fi
        
    else
        echo -e "${RED}❌ APK is corrupted or invalid${NC}"
        return 1
    fi
}

# Main validation
echo "🔍 Starting APK validation..."

# Check for APKs in standard locations
apk_locations=(
    "android-fdroid/app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk"
    "android-googleplay/app/build/outputs/apk/googleplay/debug/app-googleplay-debug.apk"
    "android-fdroid/app/build/outputs/apk/debug/app-debug.apk"
)

found_apks=0
for apk_path in "${apk_locations[@]}"; do
    if [ -f "$apk_path" ]; then
        validate_apk "$apk_path"
        ((found_apks++))
    fi
done

# Search for any APK if standard locations fail
if [ $found_apks -eq 0 ]; then
    echo -e "${YELLOW}🔍 Searching for APKs in build directories...${NC}"
    find . -name "*.apk" -type f | while read apk; do
        validate_apk "$apk"
        ((found_apks++))
    done
fi

if [ $found_apks -eq 0 ]; then
    echo -e "${RED}❌ No APKs found! Build may have failed.${NC}"
    echo "Available build outputs:"
    find . -name "build" -type d | head -10
    exit 1
else
    echo -e "\n${GREEN}✅ Found $found_apks APK(s)${NC}"
fi
