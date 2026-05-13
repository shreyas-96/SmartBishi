# PowerShell script to add fitsSystemWindows to all activity layouts
# This ensures proper display on all devices (notch, punch-hole, etc.)

$layoutPath = "app/src/main/res/layout"
$activityLayouts = Get-ChildItem -Path $layoutPath -Filter "activity_*.xml"

Write-Host "Adding fitsSystemWindows to activity layouts..." -ForegroundColor Green

foreach ($file in $activityLayouts) {
    $filePath = $file.FullName
    $content = Get-Content $filePath -Raw
    
    # Check if fitsSystemWindows already exists
    if ($content -notmatch 'android:fitsSystemWindows') {
        Write-Host "Processing: $($file.Name)" -ForegroundColor Yellow
        
        # Add fitsSystemWindows to root element
        # Pattern 1: After android:layout_height="match_parent"
        $content = $content -replace '(android:layout_height="match_parent")', '$1`n    android:fitsSystemWindows="true"'
        
        # Save the file
        Set-Content -Path $filePath -Value $content -NoNewline
        Write-Host "  ✓ Updated: $($file.Name)" -ForegroundColor Green
    } else {
        Write-Host "  ⊘ Skipped (already has fitsSystemWindows): $($file.Name)" -ForegroundColor Gray
    }
}

Write-Host "`nDone! All activity layouts updated." -ForegroundColor Green
Write-Host "Total files processed: $($activityLayouts.Count)" -ForegroundColor Cyan
