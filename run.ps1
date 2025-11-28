# Chat App - PowerShell Runner Script
# Uso: .\run.ps1 <comando>
# Comandos: server1, server2, client1, client2, build, clean, help

param(
    [Parameter(Position=0)]
    [string]$Command = "help"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

function Show-Help {
    Write-Host ""
    Write-Host "=== Chat App - Comandos Disponibles ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Build:" -ForegroundColor Yellow
    Write-Host "  .\run.ps1 build          - Compilar todo el proyecto"
    Write-Host "  .\run.ps1 build-server   - Compilar solo servidor"
    Write-Host "  .\run.ps1 build-client   - Compilar solo cliente"
    Write-Host "  .\run.ps1 clean          - Limpiar proyecto"
    Write-Host ""
    Write-Host "Ejecucion:" -ForegroundColor Yellow
    Write-Host "  .\run.ps1 server1        - Servidor 1 (puerto 8080, P2P 9090, DB 3309)"
    Write-Host "  .\run.ps1 server2        - Servidor 2 (puerto 8081, P2P 9091, DB 3310)"
    Write-Host "  .\run.ps1 client         - Ejecutar cliente"
    Write-Host "  .\run.ps1 client1        - Ejecutar cliente 1"
    Write-Host "  .\run.ps1 client2        - Ejecutar cliente 2"
    Write-Host ""
    Write-Host "Docker:" -ForegroundColor Yellow
    Write-Host "  .\run.ps1 docker-up      - Iniciar contenedores"
    Write-Host "  .\run.ps1 docker-down    - Detener contenedores"
    Write-Host ""
}

function Build-All {
    Write-Host "Compilando proyecto completo..." -ForegroundColor Green
    Set-Location $ProjectRoot
    mvn clean install -DskipTests
    Write-Host "Build completado!" -ForegroundColor Green
}

function Build-Server {
    Write-Host "Compilando servidor..." -ForegroundColor Green
    Set-Location "$ProjectRoot\chat-server"
    mvn clean package -DskipTests
    Set-Location $ProjectRoot
    Write-Host "Servidor compilado!" -ForegroundColor Green
}

function Build-Client {
    Write-Host "Compilando cliente..." -ForegroundColor Green
    Set-Location "$ProjectRoot\chat-client"
    mvn clean package -DskipTests
    Set-Location $ProjectRoot
    Write-Host "Cliente compilado!" -ForegroundColor Green
}

function Clean-Project {
    Write-Host "Limpiando proyecto..." -ForegroundColor Yellow
    Set-Location $ProjectRoot
    mvn clean
    Write-Host "Proyecto limpiado!" -ForegroundColor Green
}

function Run-Server1 {
    Write-Host "Ejecutando Servidor 1 (puerto 8080, P2P 9090, DB 3309)..." -ForegroundColor Cyan
    $jarPath = "$ProjectRoot\chat-server\server-presentation\target\server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar"
    
    if (-not (Test-Path $jarPath)) {
        Write-Host "JAR no encontrado. Compilando servidor primero..." -ForegroundColor Yellow
        Build-Server
    }
    
    java -Dserver.port=8080 -Dpeer.server.port=9090 -Ddb.port=3309 -jar $jarPath
}

function Run-Server2 {
    Write-Host "Ejecutando Servidor 2 (puerto 8081, P2P 9091, DB 3310)..." -ForegroundColor Cyan
    $jarPath = "$ProjectRoot\chat-server\server-presentation\target\server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar"
    
    if (-not (Test-Path $jarPath)) {
        Write-Host "JAR no encontrado. Compilando servidor primero..." -ForegroundColor Yellow
        Build-Server
    }
    
    java -Dserver.port=8081 -Dpeer.server.port=9091 -Ddb.port=3310 -jar $jarPath
}

function Run-Client {
    Write-Host "Ejecutando Cliente..." -ForegroundColor Cyan
    $jarPath = "$ProjectRoot\chat-client\client-presentation\target\client-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar"
    
    if (-not (Test-Path $jarPath)) {
        Write-Host "JAR no encontrado. Compilando cliente primero..." -ForegroundColor Yellow
        Build-Client
    }
    
    Start-Process java -ArgumentList "-jar", $jarPath
}

function Docker-Up {
    Write-Host "Iniciando contenedores Docker..." -ForegroundColor Green
    Set-Location "$ProjectRoot\docker"
    docker-compose -f docker-compose-test.yml up -d
    Set-Location $ProjectRoot
}

function Docker-Down {
    Write-Host "Deteniendo contenedores Docker..." -ForegroundColor Yellow
    Set-Location "$ProjectRoot\docker"
    docker-compose -f docker-compose-test.yml down
    Set-Location $ProjectRoot
}

# Main switch
switch ($Command.ToLower()) {
    "help"         { Show-Help }
    "build"        { Build-All }
    "build-server" { Build-Server }
    "build-client" { Build-Client }
    "clean"        { Clean-Project }
    "server1"      { Run-Server1 }
    "server2"      { Run-Server2 }
    "client"       { Run-Client }
    "client1"      { Run-Client }
    "client2"      { Run-Client }
    "docker-up"    { Docker-Up }
    "docker-down"  { Docker-Down }
    default        { 
        Write-Host "Comando no reconocido: $Command" -ForegroundColor Red
        Show-Help 
    }
}
