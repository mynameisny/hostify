package me.ningyu.app.hostify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig
{
    private final SecurityProperties securityProperties;

    public SecurityConfig(SecurityProperties securityProperties)
    {
        this.securityProperties = securityProperties;
    }


    @Bean
    public InMemoryUserDetailsManager userDetailsService(SecurityProperties securityProperties)
    {
        UserDetails admin = User.builder()
                .username(securityProperties.getAdmin().getUsername())
                .password(passwordEncoder().encode(securityProperties.getAdmin().getPassword()))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> authorizationManagerRequestMatcherRegistry
                        .requestMatchers("/static/**", "/webjars/**", "/swagger-ui/**", "/v3/api-docs/**", "/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/hosts/raw/**").permitAll()
                        .requestMatchers("/index.html", "/", "/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .defaultSuccessUrl("/index.html", true)
                        .successHandler((request, response, authentication) -> response.sendRedirect("/index.html"))
                        .permitAll()
                )
                .userDetailsService(userDetailsService(securityProperties));

        return http.build();
    }
}