package com.smartqueue.auth.filter;

import com.smartqueue.auth.service.JwtService;
import com.smartqueue.auth.service.SmartQueueUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final JwtService jwtService;
  private final SmartQueueUserDetailsService userDetailsService;

  public JwtAuthenticationFilter(
      JwtService jwtService, SmartQueueUserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String publicId = jwtService.extractSubject(authorization.substring(7));
      if (SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(publicId);
        if (!userDetails.isEnabled()) {
          SecurityContextHolder.clearContext();
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User account is disabled");
          return;
        }
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
      }
    } catch (JwtException | IllegalArgumentException exception) {
      SecurityContextHolder.clearContext();
      log.warn(
          "Rejected JWT for {} {}: {}",
          request.getMethod(),
          request.getRequestURI(),
          exception.getMessage());
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired access token");
      return;
    }

    filterChain.doFilter(request, response);
  }
}
