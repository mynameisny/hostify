# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Hostify is a Spring Boot-based hosts file management service that provides RESTful APIs to manage and generate hosts configurations. It's designed to serve remote hosts configurations for tools like SwitchHosts.

## Code Style

### Java Code Formatting

**Brace Style**: Always use **Allman style** (vertical alignment) for all Java code. Opening braces must be on a new line.

**Required style:**
```java
public void method()
{
    if (condition)
    {
        // code
    }
}
```

**NOT allowed:**
```java
public void method() {
    if (condition) {
        // code
    }
}
```

This applies to all code constructs: classes, methods, if statements, loops, try-catch blocks, etc.

## Build and Run Commands

### Build
```bash
./mvnw clean package
```

### Run locally

**使用H2数据库 (默认):**
```bash
./mvnw spring-boot:run
```

**使用MySQL数据库:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

或者设置环境变量：
```bash
export DATABASE_PROFILE=mysql
./mvnw spring-boot:run
```

### Run packaged JAR

**使用H2数据库 (默认):**
```bash
java -jar target/hostify-0.0.1-SNAPSHOT.jar
```

**使用MySQL数据库:**
```bash
java -jar target/hostify-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql
```

或者：
```bash
export DATABASE_PROFILE=mysql
java -jar target/hostify-0.0.1-SNAPSHOT.jar
```

### Compile without tests
```bash
./mvnw clean compile
```

## Technology Stack

- **Java 21** with Spring Boot 4.0.1
- **Spring Data JPA** with Hibernate
- **Database Support**:
  - **H2 Database** (default) - Embedded database, file-based persistence to `./data/hostify`, includes H2 Console at `/h2-console`
  - **MySQL** - Production database option via profile
- **Spring Security** with in-memory authentication
- **Spring Web MVC** with REST controllers
- **SpringDoc OpenAPI 3.0.1** for API documentation
- **Lombok** for boilerplate reduction
- **Thymeleaf** for server-side templates
- Maven for build management

## Architecture

### Layered Architecture Pattern

The codebase follows a strict layered architecture:

1. **Entity Layer** (`entity/`) - JPA entities with audit support
   - `HostsConfig` - Main configuration with name, configKey (unique identifier for URLs), description, color, and enabled flag
   - `HostsEntry` - Individual host entries with IP address, domains (space-separated), comment, sortOrder, and enabled flag
   - `ApiKey` - API authentication keys
   - Uses JPA auditing (`@EnableJpaAuditing`) for automatic `createdAt` and `updatedAt` timestamps

2. **Repository Layer** (`repository/`) - Spring Data JPA repositories
   - `HostsConfigRepository` - Uses custom queries with JOIN FETCH to eagerly load entries and avoid N+1 problems
   - `ApiKeyRepository` - API key storage

3. **Service Layer** (`service/` and `facade/service/`) - Business logic
   - `HostsService` - Core business logic for CRUD operations, validation, hosts file generation, and batch import
   - `ApiKeyService` - API key management
   - All service methods use `@Transactional` with `readOnly = true` for read operations

4. **Controller Layer** (`facade/controller/`) - REST endpoints
   - `HostsController` - Main API for hosts management (`/api/hosts/**`)
   - `ApiKeyController` - API key management (`/api/keys/**`)
   - `HomeController` - Home page (`/`)
   - `LoginController` - Login page (`/login`)

5. **DTO Layer** (`dto/`) - Request/response objects
   - Includes validation constraints using Jakarta Bean Validation

### Entity Relationships

- `HostsConfig` has a **OneToMany** relationship with `HostsEntry`
- Uses `CascadeType.ALL` and `orphanRemoval = true` for automatic cleanup
- Foreign key constraint with `ON DELETE CASCADE` in database
- Bidirectional relationship managed through helper methods `addEntry()` and `removeEntry()` in `HostsConfig`

### Key Design Patterns

1. **Repository Pattern** - Data access abstraction through Spring Data JPA
2. **DTO Pattern** - Separate request/response objects from entities
3. **Service Layer Pattern** - Business logic isolation
4. **Optimistic Locking via Auditing** - Automatic timestamp management

## Database Schema

### hosts_config table
- `id` - Primary key (auto-increment)
- `name` - Configuration name (max 100 chars, unique)
- `config_key` - URL identifier (max 50 chars, unique, alphanumeric + underscore/hyphen only)
- `description` - Optional description (max 500 chars)
- `color` - Hex color code (#RRGGBB format)
- `enabled` - Boolean flag
- `created_at`, `updated_at` - Audit timestamps

### hosts_entry table
- `id` - Primary key (auto-increment)
- `config_id` - Foreign key to hosts_config (CASCADE DELETE)
- `ip_address` - IPv4 or IPv6 address (max 45 chars)
- `domains` - Space-separated domain list (max 2000 chars)
- `comment` - Optional comment (max 500 chars)
- `enabled` - Boolean flag
- `sort_order` - Integer 0-999999 for ordering
- `created_at`, `updated_at` - Audit timestamps

## Security Configuration

Located in `SecurityConfig.java`:

- **In-memory authentication** with single admin user (username/password configured via `application-security.yml` or environment variables `ADMIN_USERNAME`/`ADMIN_PASSWORD`)
- **BCrypt password encoding**
- **Session-based authentication** with form login
- **Public endpoints**: `/api/hosts/raw/**` (for SwitchHosts integration), `/login`, static resources
- **Protected endpoints**: All `/api/**` and `/index.html` require ROLE_ADMIN
- **CSRF disabled** for REST API compatibility
- **Form login** redirects to `/index.html` on success

## API Design

### Public Endpoints

- `GET /api/hosts/raw/{configKey}` - Returns plain text hosts file content with headers and comments

### Protected Endpoints (require authentication)

**Configs:**
- `GET /api/hosts/configs` - List all configurations with entries
- `GET /api/hosts/configs/{id}` - Get single configuration by ID
- `GET /api/hosts/{configKey}` - Get configuration by config key (JSON)
- `POST /api/hosts/configs` - Create new configuration
- `PUT /api/hosts/configs/{id}` - Update configuration
- `DELETE /api/hosts/configs/{id}` - Delete configuration (must be disabled first)
- `POST /api/hosts/configs/{id}/toggle` - Enable/disable configuration
- `POST /api/hosts/configs/{id}/reorder` - Reorder entries to sequential sortOrder

**Entries:**
- `POST /api/hosts/configs/{configId}/entries` - Add entry to configuration
- `PUT /api/hosts/entries/{id}` - Update entry
- `DELETE /api/hosts/entries/{id}` - Delete entry (must be disabled or config disabled)
- `POST /api/hosts/entries/{id}/toggle` - Enable/disable entry
- `POST /api/hosts/configs/{id}/batch-import` - Batch import from hosts file or JSON

### Batch Import

Supports two file formats:

1. **Hosts file format** (.txt, .hosts) - Standard hosts file with inline comments
2. **JSON format** (.json) - Array or object with `entries` array

Conflict handling strategies:
- `skip` - Keep existing entries
- `overwrite` - Replace conflicting entries
- `abort` - Stop on first conflict

See `BATCH_IMPORT_GUIDE.md` for detailed format specifications.

## Validation Rules

Enforced in `HostsService`:

**Config validation:**
- `name` - Required, max 100 chars, must be unique
- `configKey` - Required, max 50 chars, alphanumeric + underscore/hyphen only, must be unique
- `description` - Optional, max 500 chars
- `color` - Optional, must match `#[0-9A-Fa-f]{6}` pattern

**Entry validation:**
- `ipAddress` - Required, valid IPv4 or IPv6
- `domains` - Required, max 2000 chars, space-separated valid domain names
- `comment` - Optional, max 500 chars
- `sortOrder` - Optional, 0-999999, must be unique within config
- Duplicate entries (same IP + domains combination) not allowed within same config

## Business Rules

1. **Cannot delete enabled configurations** - Must disable first
2. **Cannot delete enabled entries if parent config is enabled** - Must disable entry or config first
3. **Auto-assign sortOrder** - If not provided, uses max(existing) + 1
4. **Unique sortOrder enforcement** - No two entries in same config can have same sortOrder
5. **Domain conflict detection** - Batch import checks for existing domains

## Configuration Files

### application.yml
- Server port configuration
- Active profile selection via `DATABASE_PROFILE` environment variable (default: h2)
- Imports `application-security.yml` for security properties

### application-h2.yml (H2 Database Profile)
- H2 embedded database configuration
- **File mode** (default): Persists data to `./data/hostify` directory
- **Memory mode**: Available by uncommenting alternative URL (data lost on restart)
- H2 Console enabled at `/h2-console` for database management
- MySQL compatibility mode enabled
- **Schema initialization**: Uses `schema-h2.sql` script (H2 doesn't support MySQL COMMENT syntax)
- JPA `ddl-auto: none` - schema managed by SQL script
- Default credentials: username=sa, password=(empty)

### application-mysql.yml (MySQL Database Profile)
- MySQL datasource configuration
- Connection URL, username, password
- JPA `ddl-auto: update` auto-creates/updates schema
- `show-sql: false` in production (set to true for debugging)

### application-security.yml
- Admin username/password with environment variable fallbacks:
  - `ADMIN_USERNAME` (default: admin)
  - `ADMIN_PASSWORD` (default: admin)

## Static Resources

Frontend files in `src/main/resources/static/`:
- Bootstrap 5 CSS/JS
- Bootstrap Icons
- Marked.js for markdown rendering
- Documentation files (arch.md, BATCH_IMPORT_GUIDE.md) auto-copied via maven-resources-plugin

## Important Notes

1. **Database Selection**: The application supports both H2 (default) and MySQL databases via Spring profiles. Switch using `DATABASE_PROFILE` environment variable or `--spring.profiles.active` parameter.
2. **H2 Data Persistence**: H2 file mode stores data in `./data/hostify` directory. Ensure this directory is writable and backed up if needed.
3. **H2 vs MySQL Schema Differences**: Entity classes use MySQL `COMMENT` syntax in `columnDefinition` annotations. H2 uses a separate SQL initialization script (`schema-h2.sql`) without COMMENT syntax, as H2 doesn't fully support MySQL's COMMENT format. MySQL profile uses Hibernate's `ddl-auto=update` for automatic schema management.
4. **N+1 Query Prevention**: Always use repository methods with JOIN FETCH (e.g., `findByConfigKeyWithEntries()`) to load entries eagerly
5. **Bidirectional Relationship Management**: Use helper methods `addEntry()`/`removeEntry()` in `HostsConfig` to maintain both sides of the relationship
6. **Transaction Boundaries**: Service layer methods are transactional; controllers should not contain business logic
7. **IP Validation**: Supports both IPv4 (regex) and IPv6 (via InetAddress.getByName())
8. **Domain Validation**: Uses regex pattern for RFC-compliant domain names
9. **No Test Suite**: No test files exist currently in the project
