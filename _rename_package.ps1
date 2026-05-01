$targetDir = "app\src\main\java\com\eterultimate\eteruee"
$oldPackage = "me.rerere.rikkahub"
$newPackage = "com.eterultimate.eteruee"

# Get all Kotlin files in the new directory
$files = Get-ChildItem -Path $targetDir -Recurse -Filter "*.kt"

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $original = $content
    
    # Replace package declaration
    $content = $content -replace "package $oldPackage", "package $newPackage"
    
    # Replace imports
    $content = $content -replace "import $oldPackage", "import $newPackage"
    
    # Replace R class references (me.rerere.rikkahub.R -> com.eterultimate.eteruee.R)
    $content = $content -replace "$oldPackage\.R", "$newPackage.R"
    
    if ($content -ne $original) {
        Set-Content $file.FullName -Value $content -Encoding UTF8
        Write-Host "Updated: $($file.FullName)"
    }
}

Write-Host "Package rename complete!"
