package me.ningyu.app.hostify.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("h2")
@ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
public class H2ConsoleConfig
{
    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet()
    {
        ServletRegistrationBean<JakartaWebServlet> registration = new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2-console/*");
        registration.addInitParameter("webAllowOthers", "false");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
