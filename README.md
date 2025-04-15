# 📡 DLM Socket Server

`dlm_socket_server` is a Spring Boot application that receives real-time data from DLM devices via a TCP socket, parses and stores them across three separate databases: `dlm_in`, `dlm_main`, and `dlm_out`.

---

## 🚀 Table of Contents

- [System Requirements](#system-requirements)
- [Database Setup](#database-setup)
- [Database Configuration](#database-configuration)
- [Run the Application](#run-the-application)
- [TCP Socket Testing Guide](#tcp-socket-testing-guide)
- [Viewing Logs and Stored Data](#viewing-logs-and-stored-data)

---

## ✅ System Requirements

- Java 17 or higher
- Maven 3.6+
- MySQL 8.x
- Postman or Swagger UI for testing APIs
- A TCP socket client tool (e.g., `socket_client_test`)

---

## 🛠 Step 1: 🛠 Database Setup

Ensure that MySQL 8.x is running and accessible.
Create the following databases in MySQL:

```sql
CREATE DATABASE dlm_in CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE dlm_main CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE dlm_out CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 🛠 Step 2: Configure application.properties
Update your database credentials and connection URLs in src/main/resources/application.properties:

```properties
# IN Database
spring.datasource.in.jdbc-url=jdbc:mysql://<ip>:<port>/dlm_in?useSSL=false&serverTimezone=UTC
spring.datasource.in.username=root
spring.datasource.in.password=password_root
spring.datasource.in.driver-class-name=com.mysql.cj.jdbc.Driver

# MAIN Database
spring.datasource.main.jdbc-url=jdbc:mysql://<ip>:<port>/dlm_main?useSSL=false&serverTimezone=UTC
spring.datasource.main.username=root
spring.datasource.main.password=password_root
spring.datasource.main.driver-class-name=com.mysql.cj.jdbc.Driver

# OUT Database
spring.datasource.out.jdbc-url=jdbc:mysql://<ip>:<port>/dlm_out?useSSL=false&serverTimezone=UTC
spring.datasource.out.username=root
spring.datasource.out.password=password_root
spring.datasource.out.driver-class-name=com.mysql.cj.jdbc.Driver
```

## 🛠 Step 3: ▶️ Run the Application
```java
java -jar target/dlm_socket_server.jar
```
The application will:

Start REST API server at [http://localhost:9000/swagger-ui/index.html#](http://localhost:9000/swagger-ui/index.html#/)

Start TCP socket listener on port 9001


## 🧪 Test the TCP Socket Server

## 🔹 Step 1: Open Swagger UI: [http://localhost:9000/swagger-ui/index.html#/whitelist-controller/importWhitelist](http://localhost:9000/swagger-ui/index.html#/whitelist-controller/importWhitelist)

Use the POST /api/whitelist endpoint to add a valid IMEI:
`["352840051234567","352840051234568"]`

## 🔹 Step 2: Start socket_client_test application and connect to:
```
Host: 127.0.0.1
Port: 9001
```

## 🔹 Step 3: Send the following message: `352840051234567|220.5|5.3|0.95|ok`

This message will be:

-> Parsed and validated

-> Transformed into JSON

-> Saved into main_data table in the dlm_main database

## 🔹 Step 4: Verify Results

