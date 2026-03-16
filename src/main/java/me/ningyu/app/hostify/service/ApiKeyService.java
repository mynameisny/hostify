package me.ningyu.app.hostify.service;

import me.ningyu.app.hostify.entity.ApiKey;
import me.ningyu.app.hostify.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ApiKeyService
{
    private final ApiKeyRepository apiKeyRepository;
    
    public ApiKeyService(ApiKeyRepository apiKeyRepository)
    {
        this.apiKeyRepository = apiKeyRepository;
    }
    
    /**
     * 生成新的API Key
     */
    public String generateApiKey()
    {
        String key = UUID.randomUUID().toString();
        ApiKey apiKey = new ApiKey(key);
        apiKeyRepository.save(apiKey);
        return key;
    }
    
    /**
     * 检查API Key是否有效（存在且未被作废）
     */
    public boolean isValidApiKey(String key)
    {
        if (key == null || key.trim().isEmpty())
        {
            return false;
        }
        
        return apiKeyRepository.findByKey(key)
                .map(apiKey -> !apiKey.isRevoked())
                .orElse(false);
    }
    
    /**
     * 作废指定的API Key
     */
    public boolean revokeApiKey(String key)
    {
        return apiKeyRepository.findByKey(key)
                .map(apiKey -> {
                    if (!apiKey.isRevoked())
                    {
                        apiKey.setRevoked(true);
                        apiKey.setRevokedAt(LocalDateTime.now());
                        apiKeyRepository.save(apiKey);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }
    
    /**
     * 获取所有API Key
     */
    public List<ApiKey> getAllApiKeys()
    {
        return apiKeyRepository.findAll();
    }
}