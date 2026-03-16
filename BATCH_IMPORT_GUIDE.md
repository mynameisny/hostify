# Hostify 批量导入指南

## 支持的文件格式

Hostify 支持两种批量导入格式：

### 1. Hosts 文件格式 (.txt, .hosts)

标准的 hosts 文件格式，支持行末注释：

```hosts
# 本地开发环境
127.0.0.1 localhost local.test

192.168.1.100 dev.example.com staging.example.com # 测试服务器

::1 ipv6-localhost # IPv6本地回环

10.0.0.50 api.internal db.internal cache.internal # 内部服务集群
```

**注释处理规则：**
- 以 `#` 开头的整行会被忽略
- 行末的 `#` 后面的内容会被提取为注释
- 空行会被忽略

### 2. JSON 文件格式 (.json)

支持两种 JSON 结构：

#### 数组格式：
```json
[
  {
    "ipAddress": "127.0.0.1",
    "domains": "localhost local.test",
    "comment": "本地开发环境"
  },
  {
    "ipAddress": "192.168.1.100", 
    "domains": "dev.example.com staging.example.com",
    "comment": "测试服务器"
  }
]
```

#### 对象格式（推荐）：
```json
{
  "entries": [
    {
      "ipAddress": "127.0.0.1",
      "domains": "localhost local.test", 
      "comment": "本地开发环境"
    },
    {
      "ipAddress": "192.168.1.100",
      "domains": "dev.example.com staging.example.com",
      "comment": "测试服务器"
    }
  ]
}
```

## 冲突处理策略

当导入的条目与现有配置中的域名发生冲突时，支持三种处理方式：

1. **跳过 (Skip)** - 保留现有条目，跳过导入的冲突条目
2. **覆盖 (Overwrite)** - 删除现有条目，用新条目替换
3. **终止 (Abort)** - 发现任何冲突时立即停止整个导入过程

## 使用步骤

1. 在 Hostify 管理面板中选择一个配置
2. 点击"批量导入"按钮
3. 选择文件类型（Hosts 或 JSON）
4. 上传文件
5. 选择冲突处理策略
6. 点击"预览"查看导入内容
7. 确认无误后点击"导入"

## 注意事项

- 域名重复检查：系统会检查每个域名是否已存在于当前配置中
- IP地址验证：只接受有效的 IPv4 和 IPv6 地址
- 注释长度限制：注释不能超过 500 个字符
- 域名长度限制：单个条目的域名列表不能超过 2000 个字符