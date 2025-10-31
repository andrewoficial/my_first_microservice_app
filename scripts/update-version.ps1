# Чтение версии из pom.xml
$pomContent = Get-Content -Path "pom.xml" -Raw
if ($pomContent -match '<version>([\d.]+)</version>') {
    $version = $matches[1]
    Write-Host "Found version: $version"
    
    # Обновление версии в Inno Setup скрипте
    $issContent = Get-Content -Path "installer/script.iss" -Raw
    $issContent = $issContent -replace '#define MyAppVersion "[\d.]+"', "#define MyAppVersion `"$version`""
    $issContent = $issContent -replace 'Source: ".*Elephant-Monitor-[\d.]+\.jar"', "Source: `"target/Elephant-Monitor-$version.jar`"; DestDir: `"{app}`"; Flags: ignoreversion"
    
    Set-Content -Path "installer/script.iss" -Value $issContent
    Write-Host "Updated Inno Setup script to version $version"
} else {
    Write-Error "Could not extract version from pom.xml"
    exit 1
}