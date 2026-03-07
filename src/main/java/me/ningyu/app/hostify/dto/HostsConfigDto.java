package me.ningyu.app.hostify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import me.ningyu.app.hostify.entity.HostsConfig;
import me.ningyu.app.hostify.entity.HostsEntry;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostsConfigDto
{
    private Long id;
    private String name;
    private String description;
    private String configKey;
    private Boolean enabled;
    private String color;
    private List<HostsEntry> entries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static HostsConfigDto fromEntity(HostsConfig config)
    {
        HostsConfigDto dto = new HostsConfigDto();
        dto.id = config.getId();
        dto.name = config.getName();
        dto.description = config.getDescription();
        dto.configKey = config.getConfigKey();
        dto.enabled = config.getEnabled();
        dto.color = config.getColor();
        dto.entries = config.getEntries(); // 这里entries已经被急加载了
        dto.createdAt = config.getCreatedAt();
        dto.updatedAt = config.getUpdatedAt();
        return dto;
    }

}