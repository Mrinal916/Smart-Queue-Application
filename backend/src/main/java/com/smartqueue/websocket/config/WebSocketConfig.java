package com.smartqueue.websocket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final WebSocketAuthChannelInterceptor authInterceptor;
  private final String[] allowedOriginPatterns;

  public WebSocketConfig(
      WebSocketAuthChannelInterceptor authInterceptor,
      @Value(
              "${smartqueue.websocket.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,http://192.168.*:*}")
          String[] allowedOriginPatterns) {
    this.authInterceptor = authInterceptor;
    this.allowedOriginPatterns = allowedOriginPatterns;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOriginPatterns);
    registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOriginPatterns).withSockJS();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(authInterceptor);
  }
}
