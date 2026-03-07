package me.ningyu.app.hostify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Data
@NoArgsConstructor
public class ApiKey
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "`key`", unique = true, nullable = false)
    private String key;
    
    @Column(nullable = false)
    private boolean revoked = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    public ApiKey(String key)
    {
        this.key = key;
    }
}