# University Chat - Server

Source code for a chat application developed in Java following the principles of **Clean Architecture** and **Design Patterns**.

---

## 🏛️ Architecture

The project is divided into 4 layers to ensure separation of concerns and maintainability:

* **`domain`**: The core of the business (Entities, Value Objects).
* **`application`**: Application logic and use cases.
* **`infrastructure`**: Concrete implementations (Database, external services).
* **`presentation`**: User interface and entry points (Controllers).

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

2.  **Launch the environment with Docker:**
    This command will build the application image and start both the app and database containers.
    ```bash
    docker-compose up --build
    ```

---

## 🛠️ Tech Stack

* **Language**: Java 21
* **Database**: MySQL 8 (Docker)
* **Persistence**: JDBC
* **Hashing**: BCrypt
* **Build Tool**: Maven