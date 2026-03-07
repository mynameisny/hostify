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
public class CreateEntryRequest
{
    @NotBlank(message = "IP地址不能为空")
    @Size(max = 45, message = "IP地址长度不能超过45个字符")
    private String ipAddress;

    @NotBlank(message = "域名列表不能为空")
    @Size(max = 2000, message = "域名列表长度不能超过2000个字符")
    private String domains;

    @Size(max = 500, message = "注释长度不能超过500个字符")
    private String comment;

    private Integer sortOrder;
}