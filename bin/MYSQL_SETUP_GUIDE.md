# MySQL Database Setup & Configuration Guide

## Quick Reference

### Database Engine

- **Database:** MySQL 8.0 or later

### Connection Details

#### Default Local Development
```
Host: localhost
Port: 3306
Database: krushikranti_db
Driver: com.mysql.cj.jdbc.Driver
```

#### Environment Variables (from .env)
```env
DB_URL=jdbc:mysql://localhost:3306/krushikranti_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password
```

---

## Database Setup Steps

### 1. Install MySQL (if not already installed)

**Windows:**
```bash
# Download from https://dev.mysql.com/downloads/mysql/
# Install MySQL Community Server 8.0+
# Remember the root password during installation
```

**macOS (using Homebrew):**
```bash
brew install mysql@8.0
brew services start mysql@8.0
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install mysql-server
sudo mysql_secure_installation
```

### 2. Create the Database

```sql
-- Login to MySQL
mysql -u root -p

-- Create the database
CREATE DATABASE krushikranti_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Verify creation
SHOW DATABASES;

-- Exit
EXIT;
```

### 3. (Optional) Create a Dedicated Database User

```sql
-- Create user
CREATE USER 'krushi_app'@'localhost' IDENTIFIED BY 'secure_password_here';

-- Grant permissions
GRANT ALL PRIVILEGES ON krushikranti_db.* TO 'krushi_app'@'localhost';

-- Flush privileges
FLUSH PRIVILEGES;

-- Verify
SHOW GRANTS FOR 'krushi_app'@'localhost';
```

### 4. Verify MySQL Connection

```bash
# Test connection with root user
mysql -u root -p -h localhost -e "SELECT VERSION();"

# Test connection with dedicated user (if created)
mysql -u krushi_app -p -h localhost krushikranti_db -e "SELECT 1;"
```

---

## Application Configuration

### Option 1: Using Environment Variables (Recommended)

Create or update `.env` file in `krushikranti-backend/`:

```env
# MySQL Database Configuration
DB_URL=jdbc:mysql://localhost:3306/krushikranti_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# If using dedicated user:
# DB_USERNAME=krushi_app
# DB_PASSWORD=secure_password_here

# Other configurations
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=your_secure_jwt_secret_key
# ... other env vars
```

### Option 2: Modifying application.properties

Edit `krushikranti-backend/src/main/resources/application.properties`:

```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/krushikranti_db?createDatabaseIfNotExist=true&useSSL=false
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```

---

## Starting the Application

### Prerequisites Check
```bash
# Verify Java 21 is installed
java -version

# Verify MySQL is running
mysql -u root -p -e "SELECT 1;"

# Verify database exists
mysql -u root -p -e "SHOW DATABASES LIKE 'krushikranti_db';"
```

### Start Backend

```bash
# Navigate to backend directory
cd krushikranti-backend

# Using Maven
mvn spring-boot:run

# Or compile and run JAR
mvn clean package
java -jar target/krushikranti-backend-1.0.0-SNAPSHOT.jar
```

### Verify Startup

```bash
# Check if Health Check endpoint is responding (in another terminal)
curl http://localhost:8080/actuator/health

# Check Swagger API docs
curl http://localhost:8080/swagger-ui.html
```

---

## Database Schema Management

### Automatic Schema Creation
Hibernate automatically creates tables on first run:
```
spring.jpa.hibernate.ddl-auto=update
```

**Modes:**
- `create`: Drop all tables and recreate (⚠️ DATA LOSS)
- `create-drop`: Create on startup, drop on shutdown (⚠️ DEV ONLY)
- `update`: Create new tables, update existing (✅ RECOMMENDED for dev)
- `validate`: Validate schema matches entities (✅ PRODUCTION)

### Manual Schema Creation

If you need to manually create tables, the schema is defined in entity files:

```bash
# Generate DDL from entities
mvn hibernate:ddl

# Or use the provided schema.sql as reference
cat src/main/resources/schema.sql
```

---

## Troubleshooting

### Issue: "Connection refused" or "Cannot connect to database"

**Solution:**
```bash
# 1. Verify MySQL is running
mysql -u root -p -e "SELECT 1;"

# 2. Check connection parameters
# - Host should be 'localhost' (not 127.0.0.1 necessarily, though both work)
# - Port should be 3306 (default)
# - Database name should be 'krushikranti_db'
# - Username and password should match your MySQL setup

# 3. Verify credentials in .env or application.properties
cat krushikranti-backend/.env

# 4. Check MySQL logs for errors
# Linux/macOS: /var/log/mysql/error.log
# Windows: C:\ProgramData\MySQL\MySQL Server 8.0\Data\*.err
```

### Issue: "Unknown database 'krushikranti_db'"

**Solution:**
```bash
# Create the database
mysql -u root -p -e "CREATE DATABASE krushikranti_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Verify it was created
mysql -u root -p -e "SHOW DATABASES;"
```

### Issue: "Access denied for user 'root'@'localhost'"

**Solution:**
```bash
# Check your password is correct in .env or application.properties
# If you forgot the password, reset it:

# Windows: Run as administrator
mysql -u root
> ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
> FLUSH PRIVILEGES;
> EXIT;

# Linux/macOS:
sudo mysql -u root
> ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
> FLUSH PRIVILEGES;
> EXIT;
```

### Issue: "Incorrect string value for column 'email'"

**Solution:**
The database charset is not utf8mb4. Recreate it:
```bash
mysql -u root -p -e "DROP DATABASE krushikranti_db;"
mysql -u root -p -e "CREATE DATABASE krushikranti_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

---

## Production Deployment

### Pre-Deployment Checklist

- [ ] Change `spring.jpa.hibernate.ddl-auto` to `validate` (not `update`)
- [ ] Configure secure MySQL credentials (not root user)
- [ ] Enable MySQL SSL/TLS for remote connections
- [ ] Set up database backups
- [ ] Configure firewall to restrict MySQL port 3306
- [ ] Change default JWT secret to a secure random string
- [ ] Set up environment variables securely (not in code)
- [ ] Test database connection on production server
- [ ] Verify database character set is utf8mb4

### Production MySQL Configuration

```properties
# Use validation instead of update
spring.jpa.hibernate.ddl-auto=validate

# Enable SSL if connecting over network
spring.datasource.url=jdbc:mysql://prod-db-host:3306/krushikranti_db?useSSL=true&serverTimezone=UTC&requireSSL=true

# Use dedicated database user (NOT root)
spring.datasource.username=krushi_prod_user
spring.datasource.password=${DB_PASSWORD}  # From secure env vars, not in config

# Set connection pool parameters
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

### Backup Strategy

```bash
# Create daily backup
mysqldump -u root -p krushikranti_db > /backups/krushikranti_db_$(date +%Y%m%d).sql

# Restore from backup
mysql -u root -p krushikranti_db < /backups/krushikranti_db_20240101.sql
```

---

## Additional Resources

- [MySQL Official Documentation](https://dev.mysql.com/doc/)
- [Hibernate Documentation](https://hibernate.org/orm/documentation/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Boot Database Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#appendix.application-properties.data)

---

## Support

For issues or clarifications:
1. Check the MYSQL_REFACTORING_SUMMARY.md for detailed refactoring notes
2. Review krushikranti-backend/README.md for setup instructions
3. Check backend logs: `target/logs/application.log`
4. Enable debug logging: `logging.level.com.krushikranti=DEBUG`
