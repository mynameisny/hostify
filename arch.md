# Hostify 项目架构文档

## 项目概述

Hostify 是一个基于 Spring Boot 的 hosts 文件管理服务，提供 RESTful API 来管理和生成 hosts 配置。主要用于为 SwitchHosts 等工具提供远程 hosts 配置服务，支持多配置管理、API Key 鉴权和前端管理界面。

## 技术栈

### 后端
- **Spring Boot 4.0.1** / **Java 21**
- **Spring Web MVC** - Web 框架（`spring-boot-starter-webmvc`）
- **Spring Data JPA** + **Hibernate** - ORM
- **Spring Security** - 认证与授权（Session + Form Login）
- **Spring Validation** - 参数校验
- **SpringDoc OpenAPI 3.0.1** - API 文档（`/swagger-ui.html`）
- **Thymeleaf** - 服务端模板（登录页）
- **Lombok** - 代码简化
- **Spring Boot Actuator** - 应用监控

### 数据库
- **H2**（默认）- 内存/文件模式，开发首选；内置 H2 Console（`/h2-console`）
- **MySQL** - 生产可选，通过 `DATABASE_PROFILE=mysql` 激活

### 前端
- **Bootstrap 5** + **Bootstrap Icons**（本地静态资源）
- 原生 JS + Fetch API，无前端框架

---

## 项目结构

```
src/main/java/me/ningyu/app/hostify/
├── Application.java
├── config/
│   ├── H2ConsoleConfig.java          # 手动注册 H2 Console Servlet（@Profile("h2")）
│   ├── SecurityConfig.java           # Spring Security 配置
│   └── SecurityProperties.java       # 管理员账号配置属性
├── dto/
│   ├── BatchImportEntry.java
│   ├── BatchImportRequest.java
│   ├── CreateConfigRequest.java
│   ├── CreateEntryRequest.java
│   ├── HostsConfigDto.java
│   ├── ToggleEnabledRequest.java
│   ├── UpdateConfigRequest.java
│   └── UpdateEntryRequest.java
├── entity/
│   ├── ApiKey.java                   # API 鉴权 Key
│   ├── HostsConfig.java              # Hosts 配置（含双向关系辅助方法）
│   └── HostsEntry.java               # Hosts 条目（含 toHostsLine()）
├── exception/
│   └── BusinessException.java
├── facade/
│   ├── controller/
│   │   ├── ApiKeyController.java     # /api/security/api-keys
│   │   ├── HomeController.java       # /  →  index.html
│   │   ├── HostsController.java      # /api/hosts/**
│   │   └── LoginController.java      # /login
│   └── service/
│       └── HostsService.java         # 核心业务逻辑
├── repository/
│   ├── ApiKeyRepository.java
│   └── HostsConfigRepository.java    # 含 JOIN FETCH 自定义查询
└── service/
    └── ApiKeyService.java

src/main/resources/
├── application.yml                   # 主配置，通过 DATABASE_PROFILE 选择数据库
├── application-h2.yml                # H2 数据库配置
├── application-mysql.yml             # MySQL 数据库配置
├── application-security.yml          # 管理员账号（支持环境变量覆盖）
├── static/
│   ├── index.html                    # 前端管理界面（SPA 风格）
│   ├── favicon.svg / favicon.png
│   ├── css/  bootstrap + bootstrap-icons
│   └── js/   bootstrap.bundle + marked.min
└── templates/
    └── login.html                    # Thymeleaf 登录页
```

---

## 核心模块详解

### 实体层（Entity）

#### HostsConfig（`hosts_config` 表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| name | VARCHAR(100) UNIQUE | 配置名称 |
| config_key | VARCHAR(50) UNIQUE | URL 标识符，仅字母/数字/下划线/连字符 |
| description | VARCHAR(500) | 可选描述 |
| color | VARCHAR(7) | 十六进制颜色 `#RRGGBB` |
| enabled | BOOLEAN | 是否启用 |
| created_at / updated_at | TIMESTAMP | JPA 审计自动填充 |

- 与 `HostsEntry` 一对多（`CascadeType.ALL`、`orphanRemoval = true`）
- 提供 `addEntry()` / `removeEntry()` 辅助方法维护双向关系

#### HostsEntry（`hosts_entry` 表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| config_id | BIGINT FK | 关联配置（CASCADE DELETE）|
| ip_address | VARCHAR(45) | IPv4 或 IPv6 |
| domains | VARCHAR(2000) | 空格分隔的域名列表 |
| comment | TEXT | 可选注释 |
| enabled | BOOLEAN | 是否启用 |
| sort_order | INT | 排序序号（同配置内唯一）|
| created_at / updated_at | TIMESTAMP | JPA 审计自动填充 |

- `toHostsLine()`：生成 hosts 行格式；禁用条目输出为 `# ip domains`

#### ApiKey（`api_keys` 表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| key | VARCHAR UNIQUE | 随机生成的 API Key |
| revoked | BOOLEAN | 是否已作废 |
| created_at | TIMESTAMP | 创建时间 |
| revoked_at | TIMESTAMP | 作废时间 |

---

### 数据访问层（Repository）

#### HostsConfigRepository
- `findByConfigKeyWithEntries(configKey)` — JOIN FETCH，防止 N+1
- `findByIdWithEntries(id)` — JOIN FETCH
- `findAllWithEntries()` — JOIN FETCH
- `existsByConfigKey / existsByName` — 唯一性校验

#### ApiKeyRepository
- `findByKey(key)` — 根据 key 查找
- `findByRevoked(false)` — 获取有效 key 列表

---

### 业务逻辑层（HostsService）

| 方法 | 说明 |
|------|------|
| `getAllConfigs()` | 获取全部配置（含条目） |
| `getConfigByKey(key)` | 按 configKey 查询 |
| `getConfigById(id)` | 按 ID 查询 |
| `createConfig(...)` | 创建配置，校验唯一性 |
| `updateConfig(...)` | 更新配置 |
| `deleteConfig(id)` | 删除（须先禁用） |
| `toggleConfigEnabled(id, enabled)` | 切换启用状态 |
| `addEntry(...)` | 添加条目，自动分配 sortOrder |
| `updateEntry(...)` | 更新条目 |
| `deleteEntry(id)` | 删除（须先禁用） |
| `toggleEntryEnabled(id, enabled)` | 切换条目启用状态 |
| `reorderEntries(configId)` | 重新整理 sortOrder（从 1 连续） |
| `batchImportEntries(configId, entries, conflictAction)` | 批量导入，支持 skip/overwrite/abort |
| `replaceEntriesFromText(configId, hostsText)` | 解析 hosts 文本，**全量原子替换**所有条目 |
| `generateHostsContent(configKey)` | 生成标准 hosts 文件文本 |

**验证规则：**
- IP：IPv4 正则 + IPv6（`InetAddress.getByName`）
- 域名：RFC 标准正则，支持多域名空格分隔
- sortOrder：同配置内不得重复，范围 0–999999

---

### 安全层（SecurityConfig）

- **认证方式**：Session + Form Login（`/login`）
- **用户存储**：InMemory，单管理员账号
- **账号来源**：`application-security.yml`，支持环境变量 `ADMIN_USERNAME` / `ADMIN_PASSWORD`
- **公开端点**：`/api/hosts/raw/**`（SwitchHosts 集成），`/login`，静态资源
- **受保护端点**：`/api/**`、`/index.html`、`/` 需 `ROLE_ADMIN`
- **H2 Console**：`/h2-console/**` 放行，设置 `frameOptions(sameOrigin)` 支持 iframe
- **API Key 鉴权**：`/api/hosts/raw/{configKey}?apiKey=xxx` 支持无 Session 访问

---

## API 接口汇总

### 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/hosts/raw/{configKey}` | 获取 hosts 文件内容（text/plain），支持 `?apiKey=` |

### 受保护接口（需登录或 Session）

**配置管理（`/api/hosts`）：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/hosts` | 获取所有配置列表 |
| POST | `/api/hosts` | 创建配置 |
| GET | `/api/hosts/{configKey}` | 按 configKey 获取配置（JSON） |
| GET | `/api/hosts/id/{configId}` | 按 ID 获取配置（JSON） |
| PUT | `/api/hosts/{id}` | 更新配置 |
| DELETE | `/api/hosts/{id}` | 删除配置（须先禁用） |
| PATCH | `/api/hosts/{id}/toggle-enabled` | 切换配置启用状态 |
| POST | `/api/hosts/{configId}/reorder-entries` | 整理条目排序 |

**条目管理：**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/hosts/{configId}/entries` | 添加条目 |
| PUT | `/api/hosts/entries/{entryId}` | 更新条目 |
| DELETE | `/api/hosts/entries/{entryId}` | 删除条目（须先禁用） |
| PATCH | `/api/hosts/entries/{entryId}/toggle-enabled` | 切换条目启用状态 |
| POST | `/api/hosts/{configId}/batch-import` | 批量导入（JSON 格式） |
| POST | `/api/hosts/{configId}/entries/replace` | 文本全量替换（text/plain hosts 格式） |

**API Key 管理（`/api/security`）：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/security/api-keys` | 获取所有 API Key |
| POST | `/api/security/api-keys` | 生成新 API Key |
| DELETE | `/api/security/api-keys/{key}` | 作废 API Key |

---

## 数据库配置

### H2（默认，Profile: `h2`）
```yaml
datasource.url: jdbc:h2:mem:hostify;MODE=MYSQL;DATABASE_TO_LOWER=TRUE
ddl-auto: update
h2.console.enabled: true   # 访问 /h2-console
```
H2 Console Servlet 通过 `H2ConsoleConfig`（`JakartaWebServlet`）手动注册，绕过 Spring Boot 4 自动配置问题。

### MySQL（Profile: `mysql`）
```yaml
# 通过环境变量或 application-mysql.yml 配置连接信息
ddl-auto: update
```

### 切换方式
```bash
# H2（默认）
./mvnw spring-boot:run

# MySQL
DATABASE_PROFILE=mysql ./mvnw spring-boot:run
# 或
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

---

## 构建与运行

```bash
# 编译
./mvnw clean compile

# 打包
./mvnw clean package

# 运行（H2）
./mvnw spring-boot:run

# 运行（MySQL）
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql

# 运行 JAR
java -jar target/hostify-0.0.1-SNAPSHOT.jar
java -jar target/hostify-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql
```

---

## Docker 部署

项目根目录提供 `Dockerfile`，采用多阶段构建：

- **builder 阶段**：`maven:3.9-eclipse-temurin-21`，编译打包
- **runtime 阶段**：`eclipse-temurin:21-jre-jammy`，仅含 JRE，镜像更小
- 以非 root 系统用户 `hostify` 运行
- JVM 参数 `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`，自动感知容器内存限制
- `/app/data` 用于 H2 文件模式持久化

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DATABASE_PROFILE` | `h2` | 数据库选择：`h2` 或 `mysql` |
| `ADMIN_USERNAME` | `admin` | 管理员用户名 |
| `ADMIN_PASSWORD` | `admin` | 管理员密码 |
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/hostify?...` | MySQL 连接串（mysql 模式） |
| `MYSQL_USERNAME` | `root` | MySQL 用户名（mysql 模式） |
| `MYSQL_PASSWORD` | （空） | MySQL 密码（mysql 模式） |

### 构建与运行命令

```bash
# 构建镜像
docker build -t hostify .

# H2 模式（数据持久化到 volume）
docker run -d -p 8080:8080 \
  -e ADMIN_PASSWORD=your_password \
  -v hostify-data:/app/data \
  --name hostify hostify

# MySQL 模式
docker run -d -p 8080:8080 \
  -e DATABASE_PROFILE=mysql \
  -e MYSQL_URL=jdbc:mysql://host:3306/hostify \
  -e MYSQL_USERNAME=user \
  -e MYSQL_PASSWORD=secret \
  -e ADMIN_PASSWORD=your_password \
  --name hostify hostify
```

