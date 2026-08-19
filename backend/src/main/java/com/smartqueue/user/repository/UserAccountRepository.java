package com.smartqueue.user.repository;

import com.smartqueue.user.entity.UserAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  boolean existsByEmail(String email);

  Optional<UserAccount> findByEmail(String email);

  Optional<UserAccount> findByPublicId(UUID publicId);

  List<UserAccount> findAllByOrderByEmailAsc();
}
