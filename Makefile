# Chat App - Makefile
# Comandos útiles para desarrollo

.PHONY: help docker-up docker-down docker-clean build build-server build-client clean \
        run-server1 run-server2 run-client1 run-client2 test-env install

# Mostrar ayuda
help:
	@echo "=== Chat App - Comandos Disponibles ==="
	@echo ""
	@echo "Docker:"
	@echo "  make docker-up       - Iniciar contenedores (MySQL + VOSK)"
	@echo "  make docker-down     - Detener contenedores"
	@echo "  make docker-clean    - Detener contenedores y borrar volúmenes (DBs)"
	@echo ""
	@echo "Build:"
	@echo "  make build           - Compilar todo el proyecto"
	@echo "  make build-server    - Compilar solo servidor"
	@echo "  make build-client    - Compilar solo cliente"
	@echo "  make install         - Compilar e instalar todo (con tests)"
	@echo "  make clean           - Limpiar proyecto"
	@echo ""
	@echo "Ejecucion:"
	@echo "  make run-server1     - Ejecutar servidor 1 (puerto 8080, P2P 9090)"
	@echo "  make run-server2     - Ejecutar servidor 2 (puerto 8081, P2P 9091)"
	@echo "  make run-client1     - Ejecutar cliente 1"
	@echo "  make run-client2     - Ejecutar cliente 2"
	@echo "  make test-env        - Iniciar entorno de prueba completo"
	@echo ""

# ==================== Docker ====================

docker-up:
	@echo "Iniciando contenedores Docker..."
	cd docker && docker-compose -f docker-compose-test.yml up -d

docker-down:
	@echo "Deteniendo contenedores Docker..."
	cd docker && docker-compose -f docker-compose-test.yml down

docker-clean:
	@echo "Deteniendo contenedores y eliminando volúmenes..."
	cd docker && docker-compose -f docker-compose-test.yml down -v

# ==================== Build ====================

build:
	@echo "Compilando proyecto completo..."
	cd chat-server && mvn clean install -DskipTests
	@echo "Build completado!"

build-server:
	@echo "Compilando servidor..."
	cd chat-server && mvn clean compile -DskipTests
	@echo "Servidor compilado!"

build-client:
	@echo "Compilando cliente..."
	cd chat-client && mvn clean compile -DskipTests
	@echo "Cliente compilado!"

install:
	@echo "Instalando proyecto completo (con tests)..."
	cd chat-server && mvn clean install

clean:
	@echo "Limpiando proyecto..."
	cd chat-server && mvn clean
	cd ../chat-client && mvn clean
	@echo "Proyecto limpiado!"

# ==================== Run ====================

run-server1:
	@echo "Ejecutando Servidor 1 (puerto 8080, P2P 9090)..."
	cd chat-server/server-presentation/target && \
	java -Dserver.port=8080 -Dpeer.server.port=9090 -Ddb.port=3309 \
	     -jar server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar

run-server2:
	@echo "Ejecutando Servidor 2 (puerto 8081, P2P 9091)..."
	cd chat-server/server-presentation/target && \
	java -Dserver.port=8081 -Dpeer.server.port=9091 -Ddb.port=3310 \
	     -jar server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar

run-client1:
	@echo "Ejecutando Cliente 1..."
	cd chat-client/client-presentation/target && \
	java -jar client-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar

run-client2:
	@echo "Ejecutando Cliente 2..."
	cd chat-client/client-presentation/target && \
	java -jar client-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar

# ==================== Test Environment ====================

test-env:
	@echo "Iniciando entorno de prueba completo..."
	./run_test_env.bat
