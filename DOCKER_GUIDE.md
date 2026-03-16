# Docker 部署指南

## 前提条件

- 已安装 [Docker](https://docs.docker.com/get-docker/)（推荐 24.0+）
- 已克隆项目代码

---

## 快速开始

### 1. 构建镜像

在项目根目录（含 `Dockerfile`）执行：

```bash
docker build -t hostify .
```

首次构建会下载依赖，时间稍长；后续若 `pom.xml` 未变更，依赖层命中缓存，构建很快。

---

## H2 模式（默认，开箱即用）

适合个人使用或快速体验，数据存储在容器内的 H2 文件数据库中。

```bash
docker run -d \
  -p 8080:8080 \
  -e ADMIN_USERNAME=admin \
  -e ADMIN_PASSWORD=your_password \
  -v hostify-data:/app/data \
  --name hostify \
  hostify
```

**参数说明：**

| 参数 | 说明 |
|------|------|
| `-p 8080:8080` | 映射端口，左侧为宿主机端口，可按需修改 |
| `-e ADMIN_PASSWORD=...` | 设置管理员密码（**务必修改默认值**） |
| `-v hostify-data:/app/data` | 将 H2 数据文件挂载到 Docker volume，容器重建后数据不丢失 |
| `--name hostify` | 容器名称，便于后续管理 |

访问 `http://localhost:8080` 即可使用。

---

## MySQL 模式

适合生产环境或需要共享数据库的场景。

```bash
docker run -d \
  -p 8080:8080 \
  -e DATABASE_PROFILE=mysql \
  -e MYSQL_URL=jdbc:mysql://your-host:3306/hostify?sslMode=DISABLED \
  -e MYSQL_USERNAME=your_user \
  -e MYSQL_PASSWORD=your_password \
  -e ADMIN_USERNAME=admin \
  -e ADMIN_PASSWORD=your_password \
  --name hostify \
  hostify
```

**MySQL 需提前创建数据库：**

```sql
CREATE DATABASE hostify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动后 Hibernate 会自动建表（`ddl-auto: update`）。

---

## 环境变量一览

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DATABASE_PROFILE` | `h2` | 数据库模式：`h2` 或 `mysql` |
| `ADMIN_USERNAME` | `admin` | 管理员用户名 |
| `ADMIN_PASSWORD` | `admin` | 管理员密码，**生产环境必须修改** |
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/hostify?...` | MySQL 连接串 |
| `MYSQL_USERNAME` | `root` | MySQL 用户名 |
| `MYSQL_PASSWORD` | （空） | MySQL 密码 |

---

## 常用管理命令

```bash
# 查看运行日志
docker logs -f hostify

# 停止容器
docker stop hostify

# 启动已停止的容器
docker start hostify

# 删除容器（数据 volume 不受影响）
docker rm hostify

# 查看数据 volume
docker volume inspect hostify-data
```

---

## 更新镜像

```bash
# 重新构建
docker build -t hostify .

# 停止并删除旧容器
docker stop hostify && docker rm hostify

# 用新镜像启动（命令同上，volume 保留数据）
docker run -d -p 8080:8080 -e ADMIN_PASSWORD=your_password -v hostify-data:/app/data --name hostify hostify
```

---

## 注意事项

- **H2 Console**（`/h2-console`）仅在 `DATABASE_PROFILE=h2` 时可用，生产环境建议使用 MySQL
- 默认管理员密码为 `admin`，首次部署后请立即通过 `ADMIN_PASSWORD` 环境变量修改
- H2 数据文件存储于容器内 `/app/data`，**务必挂载 volume**，否则容器删除后数据丢失
