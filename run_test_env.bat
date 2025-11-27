@echo off
echo Starting Test Environment...

echo 1. Starting Docker Containers (2 MySQLs + 1 VOSK)...
docker-compose -f docker/docker-compose-test.yml up -d

echo 2. Building Project...
call mvn clean install -DskipTests

echo 3. Starting Server 1 (Socket: 8080, HTTP REST: 8085, P2P: 9090, DB: 3309)...
start "Server 1" cmd /k "set SERVER_PORT=8080&& set HTTP_PORT=8085&& set PEER_SERVER_PORT=9090&& set URL=jdbc:mysql://localhost:3309/server_db&& set DRIVER=com.mysql.cj.jdbc.Driver&& set USER=server&& set PASSWORD=server&& java -cp chat-server/server-presentation/target/server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar com.serverPresentation.ServerApplication"

echo 4. Starting Server 2 (Socket: 8081, HTTP REST: 8086, P2P: 9091, DB: 3310)...
start "Server 2" cmd /k "set SERVER_PORT=8081&& set HTTP_PORT=8086&& set PEER_SERVER_PORT=9091&& set URL=jdbc:mysql://localhost:3310/server_db&& set DRIVER=com.mysql.cj.jdbc.Driver&& set USER=server&& set PASSWORD=server&& java -cp chat-server/server-presentation/target/server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar com.serverPresentation.ServerApplication"

echo 5. Starting Client 1...
start "Client 1" java -cp chat-client/client-presentation/target/client-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar com.clientPresentation.ClientApplication

echo 6. Starting Client 2...
start "Client 2" java -cp chat-client/client-presentation/target/client-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar com.clientPresentation.ClientApplication

echo.
echo ========================================
echo Environment Started Successfully!
echo ========================================
echo.
echo SERVER 1:
echo   - Socket (TCP): localhost:8080
echo   - HTTP REST API: http://localhost:8085/api
echo   - P2P: localhost:9090
echo   - Database: MySQL on 3309
echo.
echo SERVER 2:
echo   - Socket (TCP): localhost:8081
echo   - HTTP REST API: http://localhost:8086/api
echo   - P2P: localhost:9091
echo   - Database: MySQL on 3310
echo.
echo HTTP REST Endpoints:
echo   - GET /api/health - Health check
echo   - GET /api/users - Get all users
echo   - GET /api/logs - Get server logs
echo.
echo NEXT STEPS:
echo   1. Connect Client 1 to localhost:8080
echo   2. Connect Client 2 to localhost:8081
echo   3. Connect Server 2 to Server 1 via P2P (localhost:9090)
echo.
pause
