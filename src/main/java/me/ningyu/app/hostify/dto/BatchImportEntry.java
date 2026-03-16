package me.ningyu.app.hostify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.Accessors;
import me.ningyu.app.hostify.entity.EntryType;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchImportEntry
{
    /** 条目类型，默认 NORMAL；COMMENT / BLANK 时忽略 ipAddress / domains */
    private EntryType entryType = EntryType.NORMAL;

    @Size(max = 45, message = "IP地址长度不能超过45个字符")
    private String ipAddress;

    @Size(max = 2000, message = "域名列表长度不能超过2000个字符")
    private String domains;

    @Size(max = 500, message = "注释长度不能超过500个字符")
    private String comment;

    private Integer sortOrder;

    private Boolean overwrite;

    private List<String> conflictingDomains;
}