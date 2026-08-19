package com.smartqueue.websocket.config;

import com.smartqueue.auth.service.JwtService;
import com.smartqueue.auth.service.SmartQueueUserDetailsService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
  private final JwtService jwtService;
  private final SmartQueueUserDetailsService userDetailsService;

  public WebSocketAuthChannelInterceptor(
      JwtService jwtService, SmartQueueUserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String header = accessor.getFirstNativeHeader("Authorization");
      if (header == null || !header.startsWith("Bearer "))
        throw new IllegalArgumentException("Missing WebSocket bearer token");
      UserDetails user =
          userDetailsService.loadUserByUsername(jwtService.extractSubject(header.substring(7)));
      accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
      return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
    return message;
  }
}
