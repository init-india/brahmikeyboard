#!/bin/bash
echo "🏗️ Testing Project Structure Integrity..."

validate_structure() {
    local project=$1
    echo "🔍 Validating $project..."
    
    critical_files=(
        "$project/app/src/main/AndroidManifest.xml"
        "$project/app/src/main/res/xml/method.xml"
        "$project/app/src/main/kotlin/com/brahmikeyboard/ime/BrahmiInputMethodService.kt"
        "$project/app/src/main/res/layout/keyboard_view.xml"
        "$project/app/build.gradle.kts"
    )
    
    for file in "${critical_files[@]}"; do
        if [ -f "$file" ]; then
            echo "✅ $file"
        else
            echo "❌ MISSING: $file"
            return 1
        fi
    done
    return 0
}

validate_structure "android-fdroid"
validate_structure "android-googleplay"
