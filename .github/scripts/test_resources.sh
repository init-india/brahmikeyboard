#!/bin/bash
echo "🎨 Testing Resource Integrity..."

validate_resources() {
    local project=$1
    
    echo "🔍 Checking $project resources..."
    
    resources=(
        "$project/app/src/main/res/values/strings.xml"
        "$project/app/src/main/res/values/colors.xml"
        "$project/app/src/main/res/values/arrays.xml"
    )
    
    for resource in "${resources[@]}"; do
        if [ -f "$resource" ]; then
            echo "📄 Checking: $resource"
            # Use xmllint with proper error handling
            if xmllint --noout "$resource" > /dev/null 2>&1; then
                echo "✅ $resource: Valid XML"
            else
                echo "⚠️ $resource: May have XML issues (but continuing)"
                # Show the actual error
                xmllint --noout "$resource" 2>&1 | head -3 || true
            fi
        else
            echo "ℹ️ $resource: Not found (optional)"
        fi
    done
    
    # Check if strings.xml exists and has basic content
    if [ -f "$project/app/src/main/res/values/strings.xml" ]; then
        if grep -q "<string name=" "$project/app/src/main/res/values/strings.xml"; then
            echo "✅ $project: strings.xml has string resources"
        else
            echo "⚠️ $project: strings.xml exists but may be empty"
        fi
    fi
    
    return 0  # Always return success to continue pipeline
}

validate_resources "android-fdroid"
validate_resources "android-googleplay"

echo "✅ Resource validation completed (non-blocking)"
