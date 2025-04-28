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

Ensure that MySQL 8.x is running and accessible. This project now using MySQL in localhost:3306
Create the following databases in MySQL:

```sql
CREATE DATABASE dlm_in CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE dlm_main CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE dlm_out CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

![image](https://github.com/user-attachments/assets/2fe95ccf-bc6e-4a29-9a24-5b233b769e0b)


### 🛠 Step 2: Configure application.properties
Update your database credentials and connection URLs in src/main/resources/application.properties:

```properties
# IN Database
spring.datasource.in.jdbc-url=jdbc:mysql://localhost:3306/dlm_in?useSSL=false&serverTimezone=UTC
spring.datasource.in.username=root
spring.datasource.in.password=root
spring.datasource.in.driver-class-name=com.mysql.cj.jdbc.Driver

# MAIN DB
spring.datasource.main.jdbc-url=jdbc:mysql://localhost:3306/dlm_main?useSSL=false&serverTimezone=UTC
spring.datasource.main.username=root
spring.datasource.main.password=root
spring.datasource.main.driver-class-name=com.mysql.cj.jdbc.Driver

# OUT DB
spring.datasource.out.jdbc-url=jdbc:mysql://localhost:3306/dlm_out?useSSL=false&serverTimezone=UTC
spring.datasource.out.username=root
spring.datasource.out.password=root
spring.datasource.out.driver-class-name=com.mysql.cj.jdbc.Driver
```

![image](https://github.com/user-attachments/assets/56f44e14-3fda-413e-9b8a-1bcd70fb1cdd)


### 🛠 Step 3: ▶️ Build the Application
```java
Install project with maven: Maven -> Install
```

![image](https://github.com/user-attachments/assets/3074bad8-a6db-456a-abcc-efe40264d8fe)

The application will created in target folder:

![image](https://github.com/user-attachments/assets/c02dbbad-d139-4b79-9239-f6c11f7af42a)


### 🛠 Step 3: ▶️ Run the Application in server
```java
Step 1: Coppy dlm_socket_server-1.0.0.jar into server
```

![image](https://github.com/user-attachments/assets/c666bd7d-7da7-40af-ab1c-a13d9adb6747)

```java
Step 2: java -jar dlm_socket_server-1.0.0.jar
```

![image](https://github.com/user-attachments/assets/338e3278-5bbf-4deb-93ac-08430d9a60c2)


The application will:

Start REST API server at [http://IP_SERVER:9000/swagger-ui/index.html#](http://localhost:9000/swagger-ui/index.html#/)

Start TCP socket listener on IP_SERVER:9001


## Test the TCP Socket Server

### 🔹 Step 1: Open Swagger UI: 

[http://IP_SERVER:9000/swagger-ui/index.html#/whitelist-controller/importWhitelist](http://localhost:9000/swagger-ui/index.html#/whitelist-controller/importWhitelist)

Use the POST /api/whitelist endpoint to add a valid IMEI:
`["352840051234567","352840051234568"]`

![image](https://github.com/user-attachments/assets/21d90879-4aa8-4c4b-aea8-cd84cc4a0a49)


### 🔹 Step 2: Start socket_client_test (tool test in project) and connect to:

![image](https://github.com/user-attachments/assets/cb511c3f-9c0e-453a-bf67-537b51616e9d)

```
Host: SERVER_IP (in this test is 127.0.0.1)
Port: 9001
```

![image](https://github.com/user-attachments/assets/2e370a83-d46f-48e8-a7b9-3367dde25767)


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
