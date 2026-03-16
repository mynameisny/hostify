package me.ningyu.app.hostify.facade.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.ningyu.app.hostify.entity.ApiKey;
import me.ningyu.app.hostify.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security")
@Tag(name = "API Key Management", description = "API Key management for SwitchHosts integration")
public class ApiKeyController
{
    private final ApiKeyService apiKeyService;
    
    public ApiKeyController(ApiKeyService apiKeyService)
    {
        this.apiKeyService = apiKeyService;
    }


    @PostMapping("/api-keys")
    @Operation(summary = "Generate new API Key")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> generateApiKey()
    {
        String apiKey = apiKeyService.generateApiKey();
        return ResponseEntity.ok(apiKey);
    }
    
    @DeleteMapping("/api-keys/{key}")
    @Operation(summary = "Revoke API Key")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Void> revokeApiKey(@PathVariable(name = "key") String key)
    {
        boolean revoked = apiKeyService.revokeApiKey(key);
        if (revoked)
        {
            return ResponseEntity.noContent().build();
        }
        else
        {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/api-keys")
    @Operation(summary = "Get all API Keys")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ApiKey>> getAllApiKeys()
    {
        List<ApiKey> apiKeys = apiKeyService.getAllApiKeys();
        return ResponseEntity.ok(apiKeys);
    }
}