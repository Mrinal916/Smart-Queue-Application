package com.smartqueue.user.repository;

import com.smartqueue.user.entity.Role;
import com.smartqueue.user.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByName(RoleName name);
}
