param(
    [ValidateSet("cmc", "rosario")]
    [string]$Escenario = "cmc"
)

$ErrorActionPreference = "Stop"

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\javac.exe"))) {
    $jdk21 = "C:\Users\Jonas\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.4.7-hotspot"
    if (Test-Path (Join-Path $jdk21 "bin\javac.exe")) {
        $env:JAVA_HOME = $jdk21
    }
}

Write-Host "Reiniciando base de datos ./data/ppaidb con perfil '$Escenario'..." -ForegroundColor Cyan
mvn -q compile exec:java "-Dexec.mainClass=com.ppai.repository.cu28.ReiniciarEscenarioBD" "-Dexec.args=$Escenario"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
Write-Host "Listo." -ForegroundColor Green
