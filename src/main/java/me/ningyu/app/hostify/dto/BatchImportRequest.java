package me.ningyu.app.hostify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchImportRequest
{
    @NotNull(message = "条目列表不能为空")
    private List<BatchImportEntry> entries;
    
    @NotBlank(message = "冲突处理方式不能为空")
    private String conflictAction;
}