# 📡 DLM Socket Server

`dlm_socket_server` is a Spring Boot application that receives real-time data from DLM devices via a TCP socket, parses and stores them across three separate databases: `dlm_in`, `dlm_main`, and `dlm_out`.

---

## 🚀 Table of Contents

- [System Details](#system-details)
- [Application Setup](#application-setup)
- [Test the TCP Socket Server](#test-the-tcp-socket-server)

---

## System Details

- Java 17 or higher
- Maven 3.6+
- MySQL 8.x
- Swagger UI for testing APIs
- A TCP socket client tool (e.g., `socket_client_test`)

---

## Application Setup

### 🛠 Step 1: 🛠 Database Setup

Ensure that MySQL 8.x is running and accessible.
Create the following databases in MySQL:

```sql
CREATE DATABASE dlm_in CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE dlm_main CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE dlm_out CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 🛠 Step 2: Configure application.properties
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

### 🛠 Step 3: ▶️ Run the Application
```java
java -jar target/dlm_socket_server.jar
```
The application will:

Start REST API server at [http://localhost:9000/swagger-ui/index.html#](http://localhost:9000/swagger-ui/index.html#/)

Start TCP socket listener on port 9001


## Test the TCP Socket Server

### 🔹 Step 1: Open Swagger UI: 

[http://localhost:9000/swagger-ui/index.html#/whitelist-controller/importWhitelist](http://localhost:9000/swagger-ui/index.html#/whitelist-controller/importWhitelist)

Use the POST /api/whitelist endpoint to add a valid IMEI:
`["352840051234567","352840051234568"]`

![image](https://github.com/user-attachments/assets/21d90879-4aa8-4c4b-aea8-cd84cc4a0a49)


### 🔹 Step 2: Start socket_client_test application and connect to:
```
Host: 127.0.0.1
Port: 9001
```

### 🔹 Step 3: Send the following message: 
`352840051234567|220.5|5.3|0.95|ok`

![image](https://github.com/user-attachments/assets/eed7a35f-f03f-4fad-bcb0-65a3a06399d8)

This message will be:

-> Parsed and validated

-> Transformed into JSON

-> Saved into main_data table in the dlm_main database

### 🔹 Step 4: Verify Results
```sql
USE dlm_in;
SELECT * FROM in_data;
```
![image](https://github.com/user-attachments/assets/1ee30944-ed0c-4fef-9d87-855f6cbc2557)



```sql
USE dlm_main;
SELECT * FROM main_data;
```
![image](https://github.com/user-attachments/assets/afe40584-06ac-4920-b8ed-7063043aa3de)
