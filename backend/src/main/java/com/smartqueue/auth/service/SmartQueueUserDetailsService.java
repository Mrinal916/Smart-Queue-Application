package com.smartqueue.auth.service;

import com.smartqueue.user.entity.UserAccount;
import com.smartqueue.user.repository.UserAccountRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmartQueueUserDetailsService implements UserDetailsService {

  private final UserAccountRepository userAccountRepository;

  public SmartQueueUserDetailsService(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String publicId) {
    UserAccount user =
        userAccountRepository
            .findByPublicId(java.util.UUID.fromString(publicId))
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return User.withUsername(user.getPublicId().toString())
        .password(user.getPasswordHash())
        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name())))
        .disabled(!user.isEnabled())
        .build();
  }
}
