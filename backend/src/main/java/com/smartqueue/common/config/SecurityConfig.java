package com.smartqueue.common.config;

import com.smartqueue.auth.filter.JwtAuthenticationFilter;
import com.smartqueue.auth.service.JwtService;
import com.smartqueue.auth.service.SmartQueueUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(JwtService jwtService, SmartQueueUserDetailsService userDetailsService) {
    this.jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        "/",
                        "/index.html",
                        "/app.js",
                        "/styles.css",
                        "/create-user",
                        "/create-user.html",
                        "/create-user.js",
                        "/reset-password",
                        "/favicon.ico",
                        "/api/v1/auth/**",
                        "/api/v1/health",
                        "/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/ws/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/queue/live-status")
                    .hasAnyRole("ADMIN", "OFFICER")
                    .requestMatchers("/api/v1/counters/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/v1/officer/**")
                    .hasRole("OFFICER")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/tokens/next",
                        "/api/v1/tokens/*/arrive",
                        "/api/v1/tokens/*/skip",
                        "/api/v1/tokens/*/recall",
                        "/api/v1/tokens/*/complete",
                        "/api/v1/tokens/*/no-show")
                    .hasRole("OFFICER")
                    .requestMatchers(HttpMethod.POST, "/api/v1/users/*/enable")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/users/*/disable")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/role")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/users/tokens")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/v1/users/**")
                    .hasAnyRole("ADMIN", "OFFICER")
                    .requestMatchers("/api/v1/analytics/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
