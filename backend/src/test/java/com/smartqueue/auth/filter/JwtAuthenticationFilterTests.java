package com.smartqueue.auth.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.smartqueue.auth.service.JwtService;
import com.smartqueue.auth.service.SmartQueueUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class JwtAuthenticationFilterTests {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatesRequestWithValidBearerToken() throws Exception {
    JwtService jwtService = Mockito.mock(JwtService.class);
    SmartQueueUserDetailsService userDetailsService =
        Mockito.mock(SmartQueueUserDetailsService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    FilterChain chain = Mockito.mock(FilterChain.class);
    String userId = UUID.randomUUID().toString();

    when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
    when(jwtService.extractSubject("valid-token")).thenReturn(userId);
    when(userDetailsService.loadUserByUsername(userId))
        .thenReturn(
            User.withUsername(userId)
                .password("unused")
                .authorities(new SimpleGrantedAuthority("ROLE_CITIZEN"))
                .build());

    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
  }
}
