package me.ningyu.app.hostify.facade.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ningyu.app.hostify.dto.BatchImportEntry;
import me.ningyu.app.hostify.entity.EntryType;
import me.ningyu.app.hostify.entity.HostsConfig;
import me.ningyu.app.hostify.entity.HostsEntry;
import me.ningyu.app.hostify.exception.BusinessException;
import me.ningyu.app.hostify.repository.HostsConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostsService
{

    private final HostsConfigRepository hostsConfigRepository;

    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$");

    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    /**
     * 获取所有配置列表
     */
    @Transactional(readOnly = true)
    public List<HostsConfig> getAllConfigs()
    {
        return hostsConfigRepository.findAllWithEntries();
    }

    /**
     * 获取配置（含条目）
     */
    @Transactional(readOnly = true)
    public HostsConfig getConfigByKey(String configKey)
    {
        if (!StringUtils.hasText(configKey))
        {
            throw new BusinessException("配置标识符不能为空");
        }
        return hostsConfigRepository.findByConfigKeyWithEntries(configKey).orElseThrow(() -> new BusinessException("配置不存在: " + configKey));
    }

    /**
     * 根据ID获取配置（含条目）
     */
    @Transactional(readOnly = true)
    public HostsConfig getConfigById(Long id)
    {
        if (id == null)
        {
            throw new BusinessException("配置ID不能为空");
        }
        return hostsConfigRepository.findByIdWithEntries(id).orElseThrow(() -> new BusinessException("配置不存在: " + id));
    }

    /**
     * 生成 hosts 文件内容
     */
    @Transactional(readOnly = true)
    public String generateHostsContent(String configKey)
    {
        HostsConfig config = getConfigByKey(configKey);

        if (!config.getEnabled())
        {
            return "# 配置已禁用: " + config.getName();
        }

        StringBuilder sb = new StringBuilder();

        // 文件头注释
        sb.append("# ").append(config.getName()).append("\n");
        if (config.getDescription() != null)
        {
            sb.append("# ").append(config.getDescription()).append("\n");
        }
        sb.append("# 更新时间: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("\n");

        // 条目列表
        for (HostsEntry entry : config.getEntries())
        {
            sb.append(entry.toHostsLine()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 创建配置
     */
    @Transactional
    public HostsConfig createConfig(String name, String configKey, String description, String color)
    {
        // 字段校验
        validateConfigFields(name, configKey, description, color);

        // 检查重复
        if (hostsConfigRepository.existsByConfigKey(configKey))
        {
            throw new BusinessException("配置标识符已存在: " + configKey);
        }
        if (hostsConfigRepository.existsByName(name))
        {
            throw new BusinessException("配置名称已存在: " + name);
        }

        HostsConfig config = new HostsConfig();
        config.setName(name);
        config.setConfigKey(configKey);
        config.setDescription(description);
        config.setColor(color);
        config.setEnabled(true);
        return hostsConfigRepository.save(config);
    }

    /**
     * 更新配置
     */
    @Transactional
    public HostsConfig updateConfig(Long id, String name, String configKey, String description, Boolean enabled, String color)
    {
        if (id == null)
        {
            throw new BusinessException("配置ID不能为空");
        }

        HostsConfig existingConfig = hostsConfigRepository.findById(id).orElseThrow(() -> new BusinessException("配置不存在: " + id));

        // 字段校验
        validateConfigFields(name, configKey, description, color);

        // 检查重复（排除当前配置）
        if (!configKey.equals(existingConfig.getConfigKey()) && hostsConfigRepository.existsByConfigKey(configKey))
        {
            throw new BusinessException("配置标识符已存在: " + configKey);
        }
        if (!name.equals(existingConfig.getName()) && hostsConfigRepository.existsByName(name))
        {
            throw new BusinessException("配置名称已存在: " + name);
        }

        existingConfig.setName(name);
        existingConfig.setConfigKey(configKey);
        existingConfig.setDescription(description);
        if (enabled != null)
        {
            existingConfig.setEnabled(enabled);
        }

        return hostsConfigRepository.save(existingConfig);
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(Long id)
    {
        if (id == null)
        {
            throw new BusinessException("配置ID不能为空");
        }

        HostsConfig config = hostsConfigRepository.findById(id).orElseThrow(() -> new BusinessException("配置不存在: " + id));

        // 检查是否启用，如果启用则不允许删除
        if (Boolean.TRUE.equals(config.getEnabled()))
        {
            throw new BusinessException("不能删除已启用的配置，请先禁用配置");
        }

        hostsConfigRepository.delete(config);
    }

    /**
     * 启用/禁用配置
     */
    @Transactional
    public HostsConfig toggleConfigEnabled(Long id, Boolean enabled)
    {
        if (id == null)
        {
            throw new BusinessException("配置ID不能为空");
        }

        HostsConfig config = hostsConfigRepository.findById(id).orElseThrow(() -> new BusinessException("配置不存在: " + id));
        config.setEnabled(enabled != null ? enabled : !config.getEnabled());
        return hostsConfigRepository.save(config);
    }

    /**
     * 添加条目
     */
    @Transactional
    public HostsEntry addEntry(Long configId, String ipAddress, String domains, String comment, Integer sortOrder)
    {
        // 字段校验（添加条目始终为 NORMAL 类型）
        validateEntryFields(EntryType.NORMAL, ipAddress, domains, comment, sortOrder);

        HostsConfig config = hostsConfigRepository.findById(configId).orElseThrow(() -> new BusinessException("配置不存在"));

        // 检查重复条目（相同IP和域名组合）
        for (HostsEntry existingEntry : config.getEntries())
        {
            if (existingEntry.getIpAddress().equals(ipAddress) && existingEntry.getDomains().equals(domains))
            {
                throw new BusinessException("条目已存在: IP=" + ipAddress + ", 域名=" + domains);
            }
        }

        // 检查排序顺序是否重复
        if (sortOrder != null)
        {
            for (HostsEntry existingEntry : config.getEntries())
            {
                if (existingEntry.getSortOrder() != null && existingEntry.getSortOrder().equals(sortOrder))
                {
                    throw new BusinessException("排序顺序 " + sortOrder + " 已被使用，请选择其他值");
                }
            }
        }

        // 如果没有指定排序顺序，自动分配最大值+1
        Integer finalSortOrder = sortOrder;
        if (finalSortOrder == null)
        {
            finalSortOrder = config.getEntries().stream()
                    .map(HostsEntry::getSortOrder)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
        }

        HostsEntry entry = new HostsEntry();
        entry.setIpAddress(ipAddress);
        entry.setDomains(domains);
        entry.setComment(comment);
        entry.setSortOrder(finalSortOrder);
        entry.setEnabled(true);

        config.addEntry(entry);
        HostsConfig savedConfig = hostsConfigRepository.save(config);
        return savedConfig.getEntries().get(savedConfig.getEntries().size() - 1);
    }

    /**
     * 更新条目
     */
    @Transactional
    public HostsEntry updateEntry(Long entryId, String ipAddress, String domains, String comment, Integer sortOrder, Boolean enabled)
    {
        if (entryId == null)
        {
            throw new BusinessException("条目ID不能为空");
        }

        // 字段校验（更新条目始终为 NORMAL 类型）
        validateEntryFields(EntryType.NORMAL, ipAddress, domains, comment, sortOrder);

        // 找到条目及其所属配置
        HostsConfig config = hostsConfigRepository.findAll().stream()
                .filter(c -> c.getEntries().stream().anyMatch(e -> e.getId().equals(entryId)))
                .findFirst()
                .orElseThrow(() -> new BusinessException("条目不存在或所属配置不存在"));

        HostsEntry entry = config.getEntries().stream()
                .filter(e -> e.getId().equals(entryId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("条目不存在"));

        // 检查重复条目（相同IP和域名组合，排除当前条目）
        for (HostsEntry existingEntry : config.getEntries())
        {
            if (!existingEntry.getId().equals(entryId) && existingEntry.getIpAddress().equals(ipAddress) && existingEntry.getDomains().equals(domains))
            {
                throw new BusinessException("条目已存在: IP=" + ipAddress + ", 域名=" + domains);
            }
        }

        // 检查排序顺序是否重复（排除当前条目）
        if (sortOrder != null)
        {
            for (HostsEntry existingEntry : config.getEntries())
            {
                if (!existingEntry.getId().equals(entryId) &&
                        existingEntry.getSortOrder() != null &&
                        existingEntry.getSortOrder().equals(sortOrder))
                {
                    throw new BusinessException("排序顺序 " + sortOrder + " 已被使用，请选择其他值");
                }
            }
        }

        entry.setIpAddress(ipAddress);
        entry.setDomains(domains);
        entry.setComment(comment);
        entry.setSortOrder(sortOrder != null ? sortOrder : entry.getSortOrder());
        if (enabled != null)
        {
            entry.setEnabled(enabled);
        }

        hostsConfigRepository.save(config);
        return entry;
    }

    /**
     * 删除条目
     */
    @Transactional
    public void deleteEntry(Long entryId)
    {
        if (entryId == null)
        {
            throw new BusinessException("条目ID不能为空");
        }

        // 找到条目及其所属配置
        HostsConfig config = hostsConfigRepository.findAll().stream()
                .filter(c -> c.getEntries().stream().anyMatch(e -> e.getId().equals(entryId)))
                .findFirst()
                .orElseThrow(() -> new BusinessException("条目不存在或所属配置不存在"));

        HostsEntry entry = config.getEntries().stream()
                .filter(e -> e.getId().equals(entryId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("条目不存在"));

        // COMMENT / BLANK 条目可以直接删除；NORMAL 条目启用时不允许删除
        if (entry.getEntryType() == EntryType.NORMAL && Boolean.TRUE.equals(entry.getEnabled()) && Boolean.TRUE.equals(config.getEnabled()))
        {
            throw new BusinessException("不能删除已启用的条目，请先禁用条目或配置");
        }

        config.removeEntry(entry);
        hostsConfigRepository.save(config);
    }

    /**
     * 启用/禁用条目
     */
    @Transactional
    public HostsEntry toggleEntryEnabled(Long entryId, Boolean enabled)
    {
        if (entryId == null)
        {
            throw new BusinessException("条目ID不能为空");
        }

        // 找到条目及其所属配置
        HostsConfig config = hostsConfigRepository.findAll().stream()
                .filter(c -> c.getEntries().stream().anyMatch(e -> e.getId().equals(entryId)))
                .findFirst()
                .orElseThrow(() -> new BusinessException("条目不存在或所属配置不存在"));

        HostsEntry entry = config.getEntries().stream()
                .filter(e -> e.getId().equals(entryId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("条目不存在"));

        if (entry.getEntryType() != EntryType.NORMAL)
        {
            throw new BusinessException("注释行和空行不支持启用/禁用操作");
        }

        entry.setEnabled(enabled != null ? enabled : !entry.getEnabled());
        hostsConfigRepository.save(config);
        return entry;
    }

    /**
     * 校验配置字段
     */
    private void validateConfigFields(String name, String configKey, String description, String color)
    {
        if (!StringUtils.hasText(name))
        {
            throw new BusinessException("配置名称不能为空");
        }
        if (name.length() > 100)
        {
            throw new BusinessException("配置名称长度不能超过100个字符");
        }
        if (!StringUtils.hasText(configKey))
        {
            throw new BusinessException("配置标识符不能为空");
        }
        if (configKey.length() > 50)
        {
            throw new BusinessException("配置标识符长度不能超过50个字符");
        }
        if (!configKey.matches("^[a-zA-Z0-9_-]+$"))
        {
            throw new BusinessException("配置标识符只能包含字母、数字、下划线和连字符");
        }
        if (description != null && description.length() > 500)
        {
            throw new BusinessException("配置描述长度不能超过500个字符");
        }
        if (color != null && !color.matches("^#[0-9A-Fa-f]{6}$"))
        {
            throw new BusinessException("颜色格式不正确，应为#RRGGBB格式");
        }
    }

    /**
     * 校验条目字段（NORMAL 类型才校验 IP / 域名）
     */
    private void validateEntryFields(EntryType entryType, String ipAddress, String domains, String comment, Integer sortOrder)
    {
        if (entryType == EntryType.NORMAL)
        {
            if (!StringUtils.hasText(ipAddress))
            {
                throw new BusinessException("IP地址不能为空");
            }
            if (isInValidIpAddress(ipAddress))
            {
                throw new BusinessException("IP地址格式不正确: " + ipAddress);
            }
            if (!StringUtils.hasText(domains))
            {
                throw new BusinessException("域名列表不能为空");
            }
            if (domains.length() > 2000)
            {
                throw new BusinessException("域名列表长度不能超过2000个字符");
            }
            if (!isValidDomains(domains))
            {
                throw new BusinessException("域名格式不正确");
            }
        }
        if (comment != null && comment.length() > 500)
        {
            throw new BusinessException("注释长度不能超过500个字符");
        }
        if (sortOrder != null && (sortOrder < 0 || sortOrder > 999999))
        {
            throw new BusinessException("排序值必须在0-999999之间");
        }
    }

    /**
     * 验证IP地址格式
     */
    /*private boolean isValidIpAddress(String ipAddress)
    {
        if (IP_PATTERN.matcher(ipAddress).matches())
        {
            try
            {
                InetAddress.getByName(ipAddress);
                return true;
            }
            catch (UnknownHostException e)
            {
                return false;
            }
        }
        return false;
    }*/

    /**
     * 验证域名格式
     */
    private boolean isValidDomains(String domains)
    {
        String[] domainArray = domains.trim().split("\\s+");
        for (String domain : domainArray)
        {
            if (!DOMAIN_PATTERN.matcher(domain).matches())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 通过纯文本全量替换条目（hosts 文件格式）
     */
    @Transactional
    public HostsConfig replaceEntriesFromText(Long configId, String hostsText)
    {
        if (configId == null)
        {
            throw new BusinessException("配置ID不能为空");
        }

        HostsConfig config = hostsConfigRepository.findByIdWithEntries(configId)
                .orElseThrow(() -> new BusinessException("配置不存在: " + configId));

        List<HostsEntry> newEntries = parseHostsText(hostsText != null ? hostsText : "");

        config.getEntries().clear();
        for (int i = 0; i < newEntries.size(); i++)
        {
            HostsEntry entry = newEntries.get(i);
            entry.setSortOrder(i + 1);
            config.addEntry(entry);
        }

        return hostsConfigRepository.save(config);
    }

    /**
     * 解析 hosts 格式文本为条目列表
     * 空行         → BLANK 条目
     * # 非IP文字   → COMMENT 条目
     * # IP 域名    → disabled NORMAL 条目
     * IP 域名      → enabled NORMAL 条目
     */
    private List<HostsEntry> parseHostsText(String text)
    {
        List<HostsEntry> entries = new java.util.ArrayList<>();
        String[] lines = text.split("\n", -1);

        for (String line : lines)
        {
            String trimmed = line.trim();

            // 空行 → BLANK
            if (!StringUtils.hasText(trimmed))
            {
                HostsEntry entry = new HostsEntry();
                entry.setEntryType(EntryType.BLANK);
                entry.setEnabled(true);
                entries.add(entry);
                continue;
            }

            // 以 # 开头
            if (trimmed.startsWith("#"))
            {
                String afterHash = trimmed.substring(1).trim();
                String[] parts = afterHash.split("\\s+");

                // # + 有效IP → disabled NORMAL
                if (parts.length >= 2 && !isInValidIpAddress(parts[0]))
                {
                    String ip = parts[0];
                    String commentInLine = null;
                    String rest = afterHash.substring(parts[0].length()).trim();
                    int hashIdx = rest.indexOf('#');
                    if (hashIdx != -1)
                    {
                        String raw = rest.substring(hashIdx + 1).trim();
                        commentInLine = raw.isEmpty() ? null : raw;
                        rest = rest.substring(0, hashIdx).trim();
                    }
                    String domains = rest.trim();
                    if (!domains.isEmpty() && !isInValidIpAddress(ip) && isValidDomains(domains))
                    {
                        HostsEntry entry = new HostsEntry();
                        entry.setEntryType(EntryType.NORMAL);
                        entry.setIpAddress(ip);
                        entry.setDomains(domains);
                        entry.setComment(commentInLine);
                        entry.setEnabled(false);
                        entries.add(entry);
                        continue;
                    }
                }

                // 其他 # 行 → COMMENT
                HostsEntry entry = new HostsEntry();
                entry.setEntryType(EntryType.COMMENT);
                entry.setComment(afterHash.isEmpty() ? null : afterHash);
                entry.setEnabled(true);
                entries.add(entry);
                continue;
            }

            // 普通行 → enabled NORMAL
            String processedLine = trimmed;
            String comment = null;
            int hashIdx = processedLine.indexOf('#');
            if (hashIdx != -1)
            {
                String raw = processedLine.substring(hashIdx + 1).trim();
                comment = raw.isEmpty() ? null : raw;
                processedLine = processedLine.substring(0, hashIdx).trim();
            }

            String[] parts = processedLine.split("\\s+");
            if (parts.length < 2)
            {
                continue;
            }

            String ip = parts[0];
            String domains = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));

            if (isInValidIpAddress(ip) || !isValidDomains(domains))
            {
                continue;
            }

            HostsEntry entry = new HostsEntry();
            entry.setEntryType(EntryType.NORMAL);
            entry.setIpAddress(ip);
            entry.setDomains(domains);
            entry.setComment(comment);
            entry.setEnabled(true);
            entries.add(entry);
        }

        return entries;
    }

    /**
     * 整理配置中的条目排序，重新分配连续的排序序号（从1开始）
     */
    @Transactional
    public HostsConfig reorderEntries(Long configId)
    {
        if (configId == null)
        {
            throw new BusinessException("配置ID不能为空");
        }

        HostsConfig config = hostsConfigRepository.findByIdWithEntries(configId)
                .orElseThrow(() -> new BusinessException("配置不存在: " + configId));

        // 按当前排序顺序排序
        config.getEntries().sort((a, b) ->
        {
            Integer sortOrderA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            Integer sortOrderB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return sortOrderA.compareTo(sortOrderB);
        });

        // 重新分配排序序号，从1开始
        for (int i = 0; i < config.getEntries().size(); i++)
        {
            config.getEntries().get(i).setSortOrder(i + 1);
        }

        return hostsConfigRepository.save(config);
    }

    /**
     * 批量导入条目
     */
    @Transactional
    public HostsConfig batchImportEntries(Long configId, List<BatchImportEntry> entries, String conflictAction)
    {
        if (configId == null)
        {
            throw new BusinessException("配置ID不能为空");
        }
        if (entries == null || entries.isEmpty())
        {
            throw new BusinessException("条目列表不能为空");
        }
        if (!"skip".equals(conflictAction) && !"overwrite".equals(conflictAction) && !"abort".equals(conflictAction))
        {
            throw new BusinessException("无效的冲突处理方式");
        }

        HostsConfig config = hostsConfigRepository.findByIdWithEntries(configId).orElseThrow(() -> new BusinessException("配置不存在: " + configId));

        // 收集现有域名映射
        java.util.Map<String, HostsEntry> existingDomainMap = new java.util.HashMap<>();
        for (HostsEntry entry : config.getEntries())
        {
            if (entry.getDomains() != null)
            {
                String[] domains = entry.getDomains().split("\\s+");
                for (String domain : domains)
                {
                    if (StringUtils.hasText(domain))
                    {
                        existingDomainMap.put(domain.trim(), entry);
                    }
                }
            }
        }

        // 处理导入的条目
        java.util.List<HostsEntry> entriesToSave = new java.util.ArrayList<>();
        java.util.Set<Long> entriesToDelete = new java.util.HashSet<>();

        for (me.ningyu.app.hostify.dto.BatchImportEntry importEntry : entries)
        {
            EntryType entryType = importEntry.getEntryType() != null ? importEntry.getEntryType() : EntryType.NORMAL;

            // COMMENT / BLANK 条目直接创建，无需冲突检测
            if (entryType != EntryType.NORMAL)
            {
                HostsEntry newEntry = new HostsEntry();
                newEntry.setEntryType(entryType);
                newEntry.setComment(importEntry.getComment());
                newEntry.setSortOrder(importEntry.getSortOrder() != null ? importEntry.getSortOrder() : config.getEntries().size() + entriesToSave.size() + 1);
                newEntry.setEnabled(true);
                newEntry.setHostsConfig(config);
                entriesToSave.add(newEntry);
                continue;
            }

            // 验证IP地址
            if (isInValidIpAddress(importEntry.getIpAddress()))
            {
                throw new BusinessException("无效的IP地址: " + importEntry.getIpAddress());
            }

            // 验证域名
            if (!StringUtils.hasText(importEntry.getDomains()))
            {
                throw new BusinessException("域名列表不能为空");
            }

            String[] newDomains = importEntry.getDomains().split("\\s+");
            boolean hasConflict = false;
            java.util.List<String> conflictingDomains = new java.util.ArrayList<>();

            // 检查冲突
            for (String domain : newDomains)
            {
                if (StringUtils.hasText(domain) && existingDomainMap.containsKey(domain.trim()))
                {
                    hasConflict = true;
                    conflictingDomains.add(domain.trim());
                }
            }

            if (hasConflict)
            {
                switch (conflictAction)
                {
                    case "abort" -> throw new BusinessException("发现重复域名，终止导入: " + String.join(", ", conflictingDomains));
                    case "skip" ->
                    {
                        // 跳过此条目
                        continue;
                    }
                    case "overwrite" ->
                    {
                        // 标记要删除的现有条目
                        for (String domain : conflictingDomains)
                        {
                            HostsEntry existingEntry = existingDomainMap.get(domain);
                            if (existingEntry != null)
                            {
                                entriesToDelete.add(existingEntry.getId());
                            }
                        }
                    }
                }
            }

            // 创建新 NORMAL 条目
            HostsEntry newEntry = new HostsEntry();
            newEntry.setEntryType(EntryType.NORMAL);
            newEntry.setIpAddress(importEntry.getIpAddress());
            newEntry.setDomains(importEntry.getDomains());
            newEntry.setComment(importEntry.getComment());
            newEntry.setSortOrder(importEntry.getSortOrder() != null ? importEntry.getSortOrder() : config.getEntries().size() + entriesToSave.size() + 1);
            newEntry.setEnabled(true);
            newEntry.setHostsConfig(config);
            entriesToSave.add(newEntry);
        }

        // 删除需要覆盖的条目
        if (!entriesToDelete.isEmpty())
        {
            config.getEntries().removeIf(entry -> entriesToDelete.contains(entry.getId()));
        }

        // 添加新条目
        config.getEntries().addAll(entriesToSave);

        return hostsConfigRepository.save(config);
    }

    /**
     * 验证IP地址格式
     */
    private boolean isInValidIpAddress(String ip)
    {
        if (!StringUtils.hasText(ip))
        {
            return true;
        }

        // IPv4 验证
        if (IP_PATTERN.matcher(ip).matches())
        {
            return false;
        }

        // IPv6 验证（简化）
        try
        {
            InetAddress inetAddress = InetAddress.getByName(ip);
            log.debug("IP地址{}成功按IPv6解析：{}", ip, inetAddress.getHostAddress());
            return false;
        }
        catch (UnknownHostException e)
        {
            return true;
        }
    }
}
