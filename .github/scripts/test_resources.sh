#!/bin/bash
echo "🎨 Testing Resource Integrity..."

validate_resources() {
    local project=$1
    
    resources=(
        "$project/app/src/main/res/values/strings.xml"
        "$project/app/src/main/res/values/colors.xml"
        "$project/app/src/main/res/values/arrays.xml"
    )
    
    for resource in "${resources[@]}"; do
        if [ -f "$resource" ]; then
            if xmllint --noout "$resource" 2>/dev/null; then
                echo "✅ $resource (valid XML)"
            else
                echo "❌ $resource (invalid XML)"
                return 1
            fi
        else
            echo "⚠️  $resource (missing but optional)"
        fi
    done
    return 0
}

validate_resources "android-fdroid"
validate_resources "android-googleplay"
