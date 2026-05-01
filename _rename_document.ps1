$targetDir = "document\src\main\java\com\eterultimate\eteruee\document"
$oldPackage = "me.rerere.document"
$newPackage = "com.eterultimate.eteruee.document"

$files = Get-ChildItem -Path $targetDir -Recurse -Filter "*.kt"

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $original = $content
    
    $content = $content -replace "package $oldPackage", "package $newPackage"
    $content = $content -replace "import $oldPackage", "import $newPackage"
    
    if ($content -ne $original) {
        Set-Content $file.FullName -Value $content -Encoding UTF8
        Write-Host "Updated: $($file.FullName)"
    }
}

Write-Host "Document package rename complete!"
