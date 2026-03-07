package me.ningyu.app.hostify.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties
{
    private Admin admin = new Admin();


    @Setter
    @Getter
    public static class Admin
    {
        private String username = "admin";
        private String password = "admin";
    }
}