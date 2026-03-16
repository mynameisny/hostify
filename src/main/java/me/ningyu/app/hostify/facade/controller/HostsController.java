package me.ningyu.app.hostify.facade.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import me.ningyu.app.hostify.dto.*;
import me.ningyu.app.hostify.exception.BusinessException;
import me.ningyu.app.hostify.facade.service.HostsService;
import me.ningyu.app.hostify.service.ApiKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hosts")
@RequiredArgsConstructor
public class HostsController
{
    private final HostsService hostsService;

    private final ApiKeyService apiKeyService;


    /**
     * 创建配置
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createConfig(@RequestBody CreateConfigRequest request)
    {
        var config = hostsService.createConfig(request.getName(), request.getConfigKey(), request.getDescription(), request.getColor());
        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConfig(@PathVariable(name = "id") Long id)
    {
        try
        {
            hostsService.deleteConfig(id);
            return ResponseEntity.noContent().build();
        }
        catch (BusinessException e)
        {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 更新配置
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateConfig(@PathVariable(name = "id") Long id, @RequestBody UpdateConfigRequest request)
    {
        var config = hostsService.updateConfig(id, request.getName(), request.getConfigKey(), request.getDescription(), request.getEnabled(), request.getColor());
        return ResponseEntity.ok(config);
    }

    /**
     * 获取所有配置列表
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllConfigs()
    {
        var configs = hostsService.getAllConfigs().stream().map(HostsConfigDto::fromEntity).toList();
        return ResponseEntity.ok(configs);
    }

    /**
     * 获取指定配置的原始 Hosts 内容（用于 SwitchHosts 等工具集成）
     * 支持通过 API Key 进行认证（无需登录）
     * URL 示例：/api/hosts/raw/{configKey}?apiKey=your-api-key
     */
    @GetMapping(value = "/raw/{configKey}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> getHostsRaw(@PathVariable(name = "configKey") String configKey, @RequestParam(name = "apiKey", required = false) String apiKey, HttpServletRequest request)
    {
        // 如果是管理员会话，直接允许访问
        if (request.getUserPrincipal() != null)
        {
            String content = hostsService.generateHostsContent(configKey);
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(content);
        }

        // 否则需要API Key验证
        if (apiKey == null || apiKey.trim().isEmpty())
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!apiKeyService.isValidApiKey(apiKey))
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String content = hostsService.generateHostsContent(configKey);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(content);
    }

    /**
     * 获取配置信息（JSON 格式，用于管理）- 通过配置标识符
     */
    @GetMapping(value = "/{configKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getConfigByKey(@PathVariable(name = "configKey") String configKey)
    {
        try
        {
            var config = hostsService.getConfigByKey(configKey);
            var dto = HostsConfigDto.fromEntity(config);
            return ResponseEntity.ok(dto);
        }
        catch (RuntimeException e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取配置信息（JSON 格式，用于管理）- 通过配置ID
     */
    @GetMapping(value = "/id/{configId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getConfigById(@PathVariable(name = "configId") Long configId)
    {
        try
        {
            var config = hostsService.getConfigById(configId);
            var dto = HostsConfigDto.fromEntity(config);
            return ResponseEntity.ok(dto);
        }
        catch (RuntimeException e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 启用/禁用配置
     */
    @PatchMapping("/{id}/toggle-enabled")
    public ResponseEntity<?> toggleConfigEnabled(@PathVariable(name = "id") Long id, @RequestBody ToggleEnabledRequest request)
    {
        var config = hostsService.toggleConfigEnabled(id, request.getEnabled());
        return ResponseEntity.ok(config);
    }

    /**
     * 添加条目
     */
    @PostMapping(value = "/{configId}/entries", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addEntry(@PathVariable(name = "configId") Long configId, @RequestBody CreateEntryRequest request)
    {
        var entry = hostsService.addEntry(configId, request.getIpAddress(), request.getDomains(), request.getComment(), request.getSortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    /**
     * 删除条目
     */
    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<?> deleteEntry(@PathVariable(name = "entryId") Long entryId)
    {
        try
        {
            hostsService.deleteEntry(entryId);
            return ResponseEntity.noContent().build();
        }
        catch (BusinessException e)
        {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 更新条目
     */
    @PutMapping(value = "/entries/{entryId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateEntry(@PathVariable(name = "entryId") Long entryId, @RequestBody UpdateEntryRequest request)
    {
        var entry = hostsService.updateEntry(entryId, request.getIpAddress(), request.getDomains(), request.getComment(), request.getSortOrder(), request.getEnabled());
        return ResponseEntity.ok(entry);
    }

    /**
     * 启用/禁用条目
     */
    @PatchMapping("/entries/{entryId}/toggle-enabled")
    public ResponseEntity<?> toggleEntryEnabled(@PathVariable(name = "entryId") Long entryId, @RequestBody ToggleEnabledRequest request)
    {
        var entry = hostsService.toggleEntryEnabled(entryId, request.getEnabled());
        return ResponseEntity.ok(entry);
    }

    /**
     * 整理配置中的条目排序
     */
    @PostMapping("/{configId}/reorder-entries")
    public ResponseEntity<?> reorderEntries(@PathVariable(name = "configId") Long configId)
    {
        try
        {
            var config = hostsService.reorderEntries(configId);
            var dto = HostsConfigDto.fromEntity(config);
            return ResponseEntity.ok(dto);
        }
        catch (RuntimeException e)
        {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 批量导入条目
     */
    @PostMapping(value = "/{configId}/batch-import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> batchImportEntries(@PathVariable(name = "configId") Long configId, @RequestBody BatchImportRequest request)
    {
        try
        {
            var config = hostsService.batchImportEntries(configId, request.getEntries(), request.getConflictAction());
            var dto = HostsConfigDto.fromEntity(config);
            return ResponseEntity.ok(dto);
        }
        catch (RuntimeException exception)
        {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }


    /**
     * 通过纯文本全量替换条目（hosts 文件格式）
     */
    @PostMapping(value = "/{configId}/entries/replace", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> replaceEntriesFromText(@PathVariable(name = "configId") Long configId, @RequestBody String hostsText)
    {
        try
        {
            var config = hostsService.replaceEntriesFromText(configId, hostsText);
            return ResponseEntity.ok(HostsConfigDto.fromEntity(config));
        }
        catch (BusinessException e)
        {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException exception)
    {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
}