@echo off
setlocal enabledelayedexpansion

REM Determine script directory
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%.") do set "SCRIPT_DIR=%%~fI"

REM Paths
set "MODELS_ROOT=%SCRIPT_DIR%..\models"
set "MODEL_DIR=%MODELS_ROOT%\vosk-model-es-0.42"
set "ZIP_URL=https://alphacephei.com/vosk/models/vosk-model-es-0.42.zip"
set "TMP_ZIP=%TEMP%\vosk-model-es-0.42.zip"

if not exist "%MODELS_ROOT%" (
  mkdir "%MODELS_ROOT%" >nul 2>&1
)

if not exist "%MODEL_DIR%" (
  echo Modelo no encontrado en "%MODEL_DIR%". Descargando...
  where curl >nul 2>&1
  if %ERRORLEVEL%==0 (
    curl -L -o "%TMP_ZIP" "%ZIP_URL%"
  ) else (
    echo curl no encontrado, usando PowerShell para descargar...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%ZIP_URL%' -OutFile '%TMP_ZIP%'"
  )
  if not exist "%TMP_ZIP" (
    echo Error: no se pudo descargar el modelo.
    exit /b 1
  )
  echo Extrayendo...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%TMP_ZIP%' -DestinationPath '%MODELS_ROOT%' -Force"
  del /f /q "%TMP_ZIP%" >nul 2>&1

  REM Normalizar nombre de carpeta si difiere
  powershell -NoProfile -ExecutionPolicy Bypass -Command "^ 
    $modelsRoot = '%MODELS_ROOT%'; ^ 
    $target = Join-Path $modelsRoot 'vosk-model-es-0.42'; ^ 
    if (-not (Test-Path $target)) { ^ 
      $cand = Get-ChildItem -Directory $modelsRoot | Where-Object { $_.Name -like 'vosk-model-es-0.42*' } | Select-Object -First 1; ^ 
      if ($cand) { Rename-Item -Path $cand.FullName -NewName 'vosk-model-es-0.42' } ^ 
    }"
  if not exist "%MODEL_DIR%" (
    echo Error: no se encontro la carpeta del modelo tras extraer.
    exit /b 1
  )
  echo Modelo listo en "%MODEL_DIR%".
) else (
  echo Modelo encontrado en "%MODEL_DIR%".
)

set "COMPOSE_FILE=%SCRIPT_DIR%..\docker\docker-compose-vosk-only.yml"
echo Levantando contenedor Vosk usando "%COMPOSE_FILE%" ...

docker compose -f "%COMPOSE_FILE%" up -d
if not %ERRORLEVEL%==0 (
  echo Error ejecutando docker compose.
  exit /b 1
)

echo Listo.
exit /b 0
