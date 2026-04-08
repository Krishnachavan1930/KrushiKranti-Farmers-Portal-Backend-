# KrushiKranti Backend

> REST API backend for the KrushiKranti Agricultural Marketplace Platform.

---

## Tech Stack

| Technology         | Version   | Notes                       |
|--------------------|-----------|---------------------------|
| Java               | 21 LTS    | OpenJDK 21 or newer       |
| Spring Boot        | 3.4.3     | Latest Spring Boot 3.x    |
| Spring Security    | (bundled) | JWT Authentication        |
| Spring Data JPA    | (bundled) | ORM with Hibernate        |
| **MySQL**          | **8.0+**  | Relational database       |
| JJWT               | 0.12.5    | JWT token generation      |
| SpringDoc OpenAPI  | 2.7.0     | Swagger/OpenAPI docs      |
| Maven              | 3.9+      | Build & dependency mgmt   |

---

## Getting Started

### Prerequisites

- Java 21 (OpenJDK 21 LTS or later)
- Maven 3.9+
- MySQL 8.0+

### Database Setup

1. **Ensure MySQL 8.0+ is installed and running**

2. **Create the database:**

```sql
CREATE DATABASE krushikranti_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

3. **Database user (Optional - customize as needed):**

```sql
CREATE USER 'krushi_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON krushikranti_db.* TO 'krushi_user'@'localhost';
FLUSH PRIVILEGES;
```

The application will automatically create tables via Hibernate's `ddl-auto=update` on first run.

### Configuration

Create or update the `.env` file in the backend root directory:

```env
# Runtime profile
SPRING_PROFILES_ACTIVE=dev

# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/krushikranti_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Kolkata
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# JWT Configuration
JWT_SECRET=your_secure_jwt_secret_key_here
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000

# Other services (Cloudinary, Razorpay, etc.)
# ... [see full config in .env.example]
```

Datasource values are now read only from environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) in `src/main/resources/application.properties`.

For Railway production, configure these environment variables in Railway project settings and set profile to prod:

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://ballast.proxy.rlwy.net:21796/railway?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
DB_USERNAME=root
DB_PASSWORD=your_railway_password
```

The backend supports both profiles with the same codebase:

```properties
# Base config
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# Profile files
# src/main/resources/application-dev.properties
# src/main/resources/application-prod.properties
```

> **⚠️ SECURITY WARNING:** Change `app.jwt.secret` to a cryptographically secure random string before deploying to production.

### Run the Application

```bash
# From the backend/ directory
mvn spring-boot:run
```

The server starts on **http://localhost:8080**

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/api/v1/api-docs
```

---

## API Endpoints Overview

### Authentication (`/api/v1/auth`)

| Method | Endpoint    | Description      | Auth Required |
|--------|-------------|------------------|---------------|
| POST   | /register   | Register user    | No            |
| POST   | /login      | Login            | No            |

### Products (`/api/v1/products`)

| Method | Endpoint             | Description          | Auth Required    |
|--------|----------------------|----------------------|------------------|
| GET    | /                    | Get all products     | No               |
| GET    | /{id}                | Get product by ID    | No               |
| GET    | /farmer/{farmerId}   | Farmer's products    | FARMER / ADMIN   |
| POST   | /                    | Create product       | FARMER           |
| PUT    | /{id}                | Update product       | FARMER / ADMIN   |
| DELETE | /{id}                | Delete product       | FARMER / ADMIN   |

### Users (`/api/v1/users`)

| Method | Endpoint  | Description          | Auth Required |
|--------|-----------|----------------------|---------------|
| GET    | /me       | Current user profile | Any logged-in |
| GET    | /         | All users            | ADMIN         |
| DELETE | /{id}     | Delete user          | ADMIN         |



---

## Roles

| Role         | Description                     |
|--------------|---------------------------------|
| ROLE_FARMER  | Can list, manage their products |
| ROLE_BUYER   | Can browse and order products   |
| ROLE_AGENT   | Intermediary between parties    |
| ROLE_ADMIN   | Full platform administration    |

---

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/krushikranti/
│   │   │   ├── KrushiKrantiApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── SwaggerConfig.java
│   │   │   │   └── CorsConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── Order.java
│   │   │   │   └── OrderItem.java
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
```
