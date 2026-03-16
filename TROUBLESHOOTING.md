# Hostify 故障排除指南

## 常见问题

### 1. SwitchHosts 客户端 SSL 证书错误

**问题描述**：
当 SwitchHosts 客户端连接使用 HTTPS 的 Hostify 服务时，出现 **"unable to verify the first certificate"** 错误。

**原因分析**：
你的远程 Hosts 服务（如 `https://hostify.dcloud.cnpc.cn/...`）使用了自签名证书或证书链不完整，而 SwitchHosts（基于 Electron/Node.js）默认会严格验证 SSL 证书。

#### 🛠️ 解决方案（按推荐程度排序）

##### ✅ 方案一：临时绕过证书验证（开发环境推荐）

在终端中设置环境变量后启动 SwitchHosts：

```bash
# 临时设置（当前终端会话有效）
export NODE_TLS_REJECT_UNAUTHORIZED=0
open /Applications/SwitchHosts.app

# 或者一行命令
NODE_TLS_REJECT_UNAUTHORIZED=0 open /Applications/SwitchHosts.app
```

⚠️ **注意**：`NODE_TLS_REJECT_UNAUTHORIZED=0` 会禁用所有 TLS 证书验证，存在中间人攻击风险，仅建议在可信网络环境下临时使用。

##### ✅ 方案二：使用 HTTP（仅限内网/测试环境）

如果远程服务支持，临时改用 `http://` 协议访问：

```bash
# 将 SwitchHosts 中的远程地址从 https:// 改为 http://
http://hostify.dcloud.cnpc.cn/api/hosts/raw/dev?apiKey=your_api_key
```

⚠️ **仅限内网或完全可信环境**，避免敏感信息泄露。

##### ✅ 方案三：配置系统信任证书（生产环境推荐）

1. **获取服务器证书**：
   ```bash
   openssl s_client -connect hostify.dcloud.cnpc.cn:443 -showcerts </dev/null 2>/dev/null | openssl x509 -outform PEM > hostify_cert.pem
   ```

2. **将证书添加到系统信任库**（macOS）：
   ```bash
   sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain hostify_cert.pem
   ```

3. **重启 SwitchHosts**。

##### ✅ 方案四：使用 SwitchHosts 的代理功能

如果 SwitchHosts 支持代理设置，可以配置通过代理访问，某些代理工具（如 Charles、Fiddler）可以处理 SSL 证书问题。

#### 🔧 根本解决方案

对于生产环境，建议为 Hostify 服务配置有效的 SSL 证书：

1. **使用 Let's Encrypt 免费证书**（推荐）
2. **购买商业 SSL 证书**
3. **使用内部 CA 签发证书**，并将 CA 根证书部署到所有客户端

### 2. 中文注释乱码问题

**问题描述**：
访问 `/api/hosts/raw/dev` API 时，返回的中文注释显示为乱码。

**解决方案**：
已通过以下配置修复：
1. 数据库连接字符集配置
2. JPA 实体字段字符集定义
3. Spring Boot 响应编码配置
4. API 端点明确设置 UTF-8 Content-Type

如果问题仍然存在，请检查：
- 数据库字符集配置
- 应用服务器编码设置
- 客户端字符编码设置

### 3. 应用启动失败：Bean 名称冲突

**问题描述**：
启动时出现错误：`The bean 'characterEncodingFilter', defined in class path resource [org/springframework/boot/servlet/autoconfigure/HttpEncodingAutoConfiguration.class], could not be registered.`

**解决方案**：
已通过以下方式解决：
1. 删除自定义的 `CharacterEncodingConfig.java`
2. 在 `application.yml` 中启用 Bean 覆盖：
   ```yaml
   spring:
     main:
       allow-bean-definition-overriding: true
   ```
3. 使用 Spring Boot 自动配置的字符编码过滤器

### 4. Docker 容器无法启动

**问题描述**：
Docker 容器启动失败或立即退出。

**排查步骤**：
1. 查看容器日志：
   ```bash
   docker logs hostify
   ```

2. 检查端口冲突：
   ```bash
   # 检查 8080 端口是否被占用
   lsof -i :8080
   ```

3. 检查环境变量配置：
   ```bash
   # 确保 ADMIN_PASSWORD 已设置
   docker run -e ADMIN_PASSWORD=your_password ...
   ```

4. 检查数据卷权限：
   ```bash
   # 确保数据卷可写
   docker volume create hostify-data
   ```

### 5. API 访问返回 401 未授权

**问题描述**：
访问 `/api/hosts/raw/{configKey}` 返回 401 错误。

**解决方案**：
1. **检查 API Key**：确保 URL 中包含正确的 `apiKey` 参数
2. **验证 API Key 状态**：在管理界面检查 API Key 是否已被撤销
3. **重新生成 API Key**：如果忘记或丢失，可以在管理界面生成新的 API Key

### 6. 数据库连接问题

#### H2 数据库
- **数据丢失**：确保使用文件模式而非内存模式
- **H2 Console 无法访问**：检查是否启用了 `DATABASE_PROFILE=h2`

#### MySQL 数据库
- **连接失败**：检查 MySQL 服务是否运行
- **权限问题**：确保数据库用户有足够权限
- **字符集问题**：创建数据库时使用：
  ```sql
  CREATE DATABASE hostify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

### 7. 批量导入失败

**常见问题**：
1. **文件格式错误**：确保使用正确的 Hosts 或 JSON 格式
2. **域名冲突**：选择合适的冲突处理策略（skip/overwrite/abort）
3. **IP 地址无效**：检查 IP 地址格式是否正确
4. **文件过大**：分批导入大型文件

### 8. 性能问题

**优化建议**：
1. **使用 MySQL 替代 H2**：生产环境建议使用 MySQL
2. **启用缓存**：考虑添加 Redis 缓存层
3. **数据库索引**：确保关键字段有索引
4. **连接池配置**：调整数据库连接池参数

## 获取更多帮助

1. **查看日志**：
   ```bash
   # 本地运行
   tail -f logs/application.log
   
   # Docker 容器
   docker logs -f hostify
   ```

2. **检查 API 文档**：
   - 访问 `http://localhost:8080/swagger-ui.html`
   - 查看 OpenAPI 规范

3. **参考其他文档**：
   - [项目架构文档](arch.md)
   - [Docker 部署指南](DOCKER_GUIDE.md)
   - [批量导入指南](BATCH_IMPORT_GUIDE.md)

4. **检查 GitHub Issues**：
   - 查看是否有类似问题报告
   - 提交新的 Issue

## 联系支持

如果以上解决方案无法解决问题，请提供以下信息：
1. 错误日志全文
2. 复现步骤
3. 环境信息（操作系统、Java 版本、数据库类型）
4. 相关配置信息