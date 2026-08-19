package com.smartqueue.auth.repository;

import com.smartqueue.auth.entity.PasswordResetToken;
import com.smartqueue.user.entity.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  void deleteByUserAndUsedAtIsNull(UserAccount user);
}
