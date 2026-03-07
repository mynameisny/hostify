package me.ningyu.app.hostify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(
        name = "hosts_config",
        indexes = {
                @Index(name = "idx_hosts_config_key", columnList = "config_key")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Comment("Hosts 配置")
public class HostsConfig
{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID，自增长")
    private Long id;
    
    /**
     * 配置名称
     */
    @Column(name = "name", nullable = false, length = 100)
    @Comment("配置名称")
    private String name;
    
    /**
     * 配置描述
     */
    @Lob
    @Column(name = "description")
    @Comment("配置描述")
    private String description;
    
    /**
     * 配置标识符（用于 URL 路径）
     */
    @Column(name = "config_key", unique = true, length = 50)
    @Comment("配置标识符（用于 URL 路径）")
    private String configKey;
    
    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @Comment("是否启用")
    private Boolean enabled = true;
    
    /**
     * 配置颜色（RGB值，格式：#RRGGBB）
     */
    @Column(name = "color", length = 7)
    @Comment("配置颜色（RGB值，格式：#RRGGBB）")
    private String color = "#0d6efd";
    
    /**
     * 关联的 hosts 条目
     */
    @OneToMany(
            mappedBy = "hostsConfig",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<HostsEntry> entries = new ArrayList<>();
    
    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    @Comment("更新时间")
    private LocalDateTime updatedAt;
    
    /**
     * 添加条目
     */
    public void addEntry(HostsEntry entry)
    {
        entries.add(entry);
        entry.setHostsConfig(this);
    }
    
    /**
     * 移除条目
     */
    public void removeEntry(HostsEntry entry)
    {
        entries.remove(entry);
        entry.setHostsConfig(null);
    }
}