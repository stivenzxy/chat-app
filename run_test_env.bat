@echo off
echo Starting Test Environment...

echo 1. Starting Docker Containers (2 MySQLs + 1 VOSK)...
docker-compose -f docker/docker-compose-test.yml up -d

echo 2. Building Project...
call mvn clean install -DskipTests

echo 3. Starting Server 1 (Port 8080, P2P 9090, DB 3309)...
start "Server 1" cmd /k "set SERVER_PORT=8080&& set PEER_SERVER_PORT=9090&& set URL=jdbc:mysql://localhost:3309/server_db&& set DRIVER=com.mysql.cj.jdbc.Driver&& set USER=server&& set PASSWORD=server&& java -cp chat-server/server-presentation/target/server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar com.serverPresentation.ServerApplication"

echo 4. Starting Server 2 (Port 8081, P2P 9091, DB 3310)...
start "Server 2" cmd /k "set SERVER_PORT=8081&& set PEER_SERVER_PORT=9091&& set URL=jdbc:mysql://localhost:3310/server_db&& set DRIVER=com.mysql.cj.jdbc.Driver&& set USER=server&& set PASSWORD=server&& java -cp chat-server/server-presentation/target/server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar com.serverPresentation.ServerApplication"

echo 5. Starting Client 1...
start "Client 1" java -cp chat-client/client-presentation/target/client-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar com.clientPresentation.ClientApplication

echo 6. Starting Client 2...
start "Client 2" java -cp chat-client/client-presentation/target/client-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar com.clientPresentation.ClientApplication

echo Environment Started!
echo Connect Client 1 to localhost:8080
echo Connect Client 2 to localhost:8081
echo Connect Server 2 to Server 1 via P2P (localhost:9090)
pause
