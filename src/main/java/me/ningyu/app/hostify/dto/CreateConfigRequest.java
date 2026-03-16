package me.ningyu.app.hostify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateConfigRequest
{
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 100, message = "配置名称长度不能超过100个字符")
    private String name;
    
    @NotBlank(message = "配置标识符不能为空")
    @Size(max = 50, message = "配置标识符长度不能超过50个字符")
    private String configKey;
    
    @Size(max = 500, message = "配置描述长度不能超过500个字符")
    private String description;
    
    @Size(max = 7, message = "颜色格式不正确")
    @Builder.Default
    private String color = "#0d6efd";
}