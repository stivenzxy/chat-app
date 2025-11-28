# Script para descargar el modelo español de VOSK
# Modelo: vosk-model-small-es-0.42 (Español, ~40MB)

$modelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"
$downloadPath = ".\vosk-model\model.zip"
$extractPath = ".\vosk-model"

Write-Host "======================================"
Write-Host "Descargando modelo VOSK en espanol..."
Write-Host "======================================"
Write-Host ""
Write-Host "Modelo: vosk-model-small-es-0.42"
Write-Host "Tamano: ~40 MB"
Write-Host "URL: $modelUrl"
Write-Host ""

# Crear directorio si no existe
if (-not (Test-Path $extractPath)) {
    New-Item -ItemType Directory -Force -Path $extractPath | Out-Null
}

# Descargar el modelo
Write-Host "Descargando modelo..."
try {
    Invoke-WebRequest -Uri $modelUrl -OutFile $downloadPath -UseBasicParsing
    Write-Host "Descarga completada" -ForegroundColor Green
}
catch {
    Write-Host "Error al descargar: $_" -ForegroundColor Red
    exit 1
}

# Extraer el archivo ZIP
Write-Host ""
Write-Host "Extrayendo modelo..."
try {
    Expand-Archive -Path $downloadPath -DestinationPath $extractPath -Force
    Write-Host "Extraccion completada" -ForegroundColor Green
}
catch {
    Write-Host "Error al extraer: $_" -ForegroundColor Red
    exit 1
}

# Limpiar archivo ZIP
Remove-Item $downloadPath -Force

# Renombrar el directorio del modelo a "model"
$extractedModelPath = Join-Path $extractPath "vosk-model-small-es-0.42"
$finalModelPath = Join-Path $extractPath "model"

if (Test-Path $finalModelPath) {
    Remove-Item $finalModelPath -Recurse -Force
}

Rename-Item -Path $extractedModelPath -NewName "model"

Write-Host ""
Write-Host "======================================"
Write-Host "Modelo instalado correctamente" -ForegroundColor Green
Write-Host "======================================"
Write-Host ""
Write-Host "Ubicacion: $finalModelPath"
Write-Host ""
Write-Host "Ahora puedes iniciar VOSK ejecutando:"
Write-Host "docker-compose -f docker/docker-compose-test.yml up -d vosk-server" -ForegroundColor Cyan
