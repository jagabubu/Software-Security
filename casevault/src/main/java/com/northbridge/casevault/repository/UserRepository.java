package com.northbridge.casevault.repository;

import com.northbridge.casevault.model.Role;
import com.northbridge.casevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findAllByRoleAndEnabledTrue(Role role);
}