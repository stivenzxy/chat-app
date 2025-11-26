# University Chat - Server

Source code for a chat application developed in Java following the principles of **Clean Architecture** and **Design Patterns**.

> **🆕 NEW: REST API Endpoints Available!**  
> El servidor ahora expone endpoints HTTP REST para integración con Angular a través de Traefik.  
> 📚 **[Ver Documentación Completa](./INDEX.md)**

---

## 🏛️ Architecture

The project is divided into 4 layers to ensure separation of concerns and maintainability:

* **`domain`**: The core of the business (Entities, Value Objects).
* **`application`**: Application logic and use cases.
* **`infrastructure`**: Concrete implementations (Database, external services).
* **`presentation`**: User interface and entry points (Controllers, HTTP REST API).

---

## 🌐 REST API Endpoints

El servidor expone los siguientes endpoints HTTP REST:

* **GET /api/users** - Lista de usuarios registrados
* **GET /api/logs** - Logs del servidor (con filtrado opcional)
* **GET /api/health** - Health check endpoint

**Acceso:**
- Directo: `http://localhost:8080/api/*`
- A través de Traefik: `http://localhost/api/*`

📖 **Documentación completa**: [REST_API_DOCUMENTATION.md](./REST_API_DOCUMENTATION.md)  
🎨 **Guía de integración Angular**: [ANGULAR_INTEGRATION_GUIDE.md](./ANGULAR_INTEGRATION_GUIDE.md)  
🛠️ **Comandos útiles**: [QUICK_COMMANDS.md](./QUICK_COMMANDS.md)

---

## 🚀 Getting Started

**Prerequisites:**
* JDK 21+
* Maven 3.6+
* Docker & Docker Compose

**Steps to run:**

1.  **Build the project:**
    ```bash
    mvn clean install
    ```

2.  **Start MySQL:**
    ```bash
    cd ../docker
    docker-compose up -d mysql
    ```

3.  **Start the server:**
    ```bash
    cd server-presentation
    java -jar target/server-presentation-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```

4.  **Verify endpoints:**
    ```bash
    curl http://localhost:8080/api/health
    ```

**Con Traefik (opcional):**

1.  **Start Traefik:**
    ```bash
    cd ../../../traefik
    docker-compose up -d
    ```

2.  **Access through API Gateway:**
    ```bash
    curl http://localhost/api/health
    ```

---

## 🛠️ Tech Stack

* **Language**: Java 21
* **Database**: MySQL 8 (Docker)
* **Persistence**: JDBC
* **Hashing**: BCrypt
* **HTTP Server**: Javalin 6.1.3
* **JSON**: Jackson 2.17.0
* **Logging**: SLF4J + Logback
* **Build Tool**: Maven

---

## 📚 Documentation

* **[INDEX.md](./INDEX.md)** - Índice de toda la documentación
* **[README_TRAEFIK_INTEGRATION.md](./README_TRAEFIK_INTEGRATION.md)** - Resumen ejecutivo de integración
* **[REST_API_DOCUMENTATION.md](./REST_API_DOCUMENTATION.md)** - Documentación completa de API
* **[ARCHITECTURE_DIAGRAM.md](./ARCHITECTURE_DIAGRAM.md)** - Diagramas de arquitectura
* **[ANGULAR_INTEGRATION_GUIDE.md](./ANGULAR_INTEGRATION_GUIDE.md)** - Guía de integración con Angular
* **[QUICK_COMMANDS.md](./QUICK_COMMANDS.md)** - Comandos útiles para desarrollo
* **[VALIDATION_CHECKLIST.md](./VALIDATION_CHECKLIST.md)** - Checklist de validación

---

## 🎯 Features

### Core Features
- ✅ Registro y autenticación de usuarios
- ✅ Chat en tiempo real (TCP/Sockets)
- ✅ Replicación P2P entre servidores
- ✅ Persistencia en MySQL
- ✅ GUI Swing para administración

### REST API (Nuevo)
- ✅ Endpoints HTTP REST
- ✅ Integración con Traefik
- ✅ Compatible con Angular
- ✅ CORS habilitado
- ✅ JSON responses
- ✅ Health monitoring

---

## 🔧 Development

### Project Structure
```
chat-server/
├── server-domain/           # Entidades, Value Objects, Interfaces
├── server-application/      # Use Cases, DTOs, Mappers
├── server-infrastructure/   # Implementaciones, Persistencia
└── server-presentation/     # Controllers (HTTP + GUI), Views
```

### Running Tests
```bash
mvn test
```

### Packaging
```bash
mvn clean package
```

---

## 🌟 Architecture Highlights

- ✅ **Clean Architecture** - Separation of concerns
- ✅ **SOLID Principles** - Maintainable and extensible code
- ✅ **Design Patterns** - Factory, Repository, Observer, etc.
- ✅ **Dependency Inversion** - All dependencies point inward
- ✅ **Testability** - Easy to unit test each layer

---

## 📊 Ports and Services

| Service | Port | Description |
|---------|------|-------------|
| HTTP REST API | 8080 | REST endpoints for Angular |
| TCP Chat Server | Configured | Socket-based chat protocol |
| MySQL Database | 3307 | Data persistence |
| Traefik (Gateway) | 80 | API Gateway (optional) |
| Traefik Dashboard | 8080 | Monitoring dashboard |

---

## 🤝 Contributing

Este proyecto sigue Clean Architecture y SOLID. Al contribuir:

1. Respeta la separación de capas
2. No mezcles responsabilidades
3. Escribe tests para nuevas funcionalidades
4. Documenta cambios significativos

---

## 📝 License

Universidad de los Llanos - Arquitectura de Software

---

**Última actualización**: Noviembre 2025  
**Versión**: 1.0.0  
**Estado**: ✅ Producción