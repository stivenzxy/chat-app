@echo off
REM Chat App - Quick Commands (Windows Batch)
REM Comandos rápidos para desarrollo

if "%1"=="" goto :help
if "%1"=="help" goto :help
if "%1"=="docker-up" goto :docker-up
if "%1"=="docker-down" goto :docker-down
if "%1"=="build" goto :build
if "%1"=="build-server" goto :build-server
if "%1"=="build-client" goto :build-client
if "%1"=="clean" goto :clean
if "%1"=="server1" goto :server1
if "%1"=="server2" goto :server2
if "%1"=="client1" goto :client1
if "%1"=="client2" goto :client2
if "%1"=="test-env" goto :test-env

echo Comando no reconocido: %1
goto :help

:help
echo.
echo === Chat App - Comandos Disponibles ===
echo.
echo Docker:
echo   quick-commands docker-up      - Iniciar contenedores (MySQL + VOSK)
echo   quick-commands docker-down    - Detener contenedores
echo.
echo Build:
echo   quick-commands build           - Compilar todo el proyecto
echo   quick-commands build-server    - Compilar solo servidor
echo   quick-commands build-client    - Compilar solo cliente
echo   quick-commands clean           - Limpiar proyecto
echo.
echo Ejecucion:
echo   quick-commands server1         - Ejecutar servidor 1 (puerto 8080, P2P 9090)
echo   quick-commands server2         - Ejecutar servidor 2 (puerto 8081, P2P 9091)
echo   quick-commands client1         - Ejecutar cliente 1
echo   quick-commands client2         - Ejecutar cliente 2
echo   quick-commands test-env        - Iniciar entorno de prueba completo
echo.
goto :eof

:docker-up
echo Iniciando contenedores Docker...
cd docker
docker-compose -f docker-compose-test.yml up -d
cd ..
goto :eof

:docker-down
echo Deteniendo contenedores Docker...
cd docker
docker-compose -f docker-compose-test.yml down
cd ..
goto :eof

:build
echo Compilando proyecto completo...
cd chat-server
call mvn clean install -DskipTests
cd ..
echo Build completado!
goto :eof

:build-server
echo Compilando servidor...
cd chat-server
call mvn clean compile -DskipTests
cd ..
echo Servidor compilado!
goto :eof

:build-client
echo Compilando cliente...
cd chat-client
call mvn clean compile -DskipTests
cd ..
echo Cliente compilado!
goto :eof

:clean
echo Limpiando proyecto...
cd chat-server
call mvn clean
cd ..\chat-client
call mvn clean
cd ..
echo Proyecto limpiado!
goto :eof

:server1
echo Ejecutando Servidor 1 (puerto 8080, P2P 9090)...
cd chat-server\server-presentation\target
java -Dserver.port=8080 -Dpeer.server.port=9090 -Ddb.port=3309 -jar server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar
cd ..\..\..
goto :eof

:server2
echo Ejecutando Servidor 2 (puerto 8081, P2P 9091)...
cd chat-server\server-presentation\target
java -Dserver.port=8081 -Dpeer.server.port=9091 -Ddb.port=3310 -jar server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar
cd ..\..\..
goto :eof

:client1
echo Ejecutando Cliente 1...
cd chat-client\client-presentation\target
start java -jar client-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar
cd ..\..\..
goto :eof

:client2
echo Ejecutando Cliente 2...
cd chat-client\client-presentation\target
start java -jar client-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar
cd ..\..\..
goto :eof

:test-env
echo Iniciando entorno de prueba completo...
call run_test_env.bat
goto :eof
