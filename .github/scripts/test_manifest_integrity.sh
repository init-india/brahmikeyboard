#!/bin/bash
echo "📄 Testing Manifest Integrity..."

check_manifest() {
    local project=$1
    local manifest="$project/app/src/main/AndroidManifest.xml"
    
    echo "🔍 Checking $manifest"
    
    # Check IME service declaration
    if grep -q "android.view.InputMethod" "$manifest" && \
       grep -q "BIND_INPUT_METHOD" "$manifest"; then
        echo "✅ IME service properly declared"
    else
        echo "❌ IME service declaration incomplete"
        return 1
    fi
    
    # Check package name
    if grep -q "package=" "$manifest"; then
        echo "✅ Package declaration found"
    else
        echo "❌ Package declaration missing"
        return 1
    fi
    
    return 0
}

check_manifest "android-fdroid"
check_manifest "android-googleplay"
