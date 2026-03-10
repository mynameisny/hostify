package me.ningyu.app.hostify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "hosts_entry")
@EntityListeners(AuditingEntityListener.class)
@Comment("Hosts 条目")
public class HostsEntry
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID，自增长")
    private Long id;

    /**
     * 关联的配置
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    @Comment("关联的配置ID")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private HostsConfig hostsConfig;

    /**
     * 条目类型：NORMAL / COMMENT / BLANK
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    @Comment("条目类型：NORMAL 正常行 / COMMENT 注释行 / BLANK 空行")
    private EntryType entryType = EntryType.NORMAL;

    /**
     * IP 地址（仅 NORMAL 类型使用）
     */
    @Column(name = "ip_address", length = 45)
    @Comment("IP地址")
    private String ipAddress;

    /**
     * 域名列表（空格分隔，仅 NORMAL 类型使用）
     */
    @Column(name = "domains", length = 2000)
    @Comment("域名列表（空格分隔）")
    private String domains;

    /**
     * 注释内容（NORMAL 为行末注释，COMMENT 为注释行全文）
     */
    @Lob
    @Column(name = "comment")
    @Comment("注释内容")
    private String comment;

    /**
     * 是否启用该条目（仅对 NORMAL 类型有意义）
     */
    @Column(name = "enabled", nullable = false)
    @Comment("是否启用该条目")
    private Boolean enabled = true;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order", nullable = false)
    @Comment("排序顺序")
    private Integer sortOrder = 0;

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
     * 转换为 hosts 格式的一行
     * BLANK   → ""
     * COMMENT → "# <comment>"
     * NORMAL  → "ip domains [# comment]"，disabled 时加 # 前缀
     */
    public String toHostsLine()
    {
        if (entryType == EntryType.BLANK)
        {
            return "";
        }

        if (entryType == EntryType.COMMENT)
        {
            return "# " + (comment != null ? comment : "");
        }

        // NORMAL
        if (!Boolean.TRUE.equals(enabled))
        {
            return "# " + ipAddress + " " + domains + (comment != null ? " # " + comment : "");
        }

        return ipAddress + " " + domains + (comment != null ? " # " + comment : "");
    }
}
