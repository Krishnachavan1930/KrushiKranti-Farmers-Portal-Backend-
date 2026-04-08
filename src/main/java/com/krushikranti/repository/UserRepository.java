package com.krushikranti.repository;

import com.krushikranti.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Find users by role
    List<User> findByRole(User.Role role);

    Page<User> findByRole(User.Role role, Pageable pageable);

    // Find active users by role
    List<User> findByRoleAndEnabled(User.Role role, boolean enabled);

    Page<User> findByRoleAndEnabled(User.Role role, boolean enabled, Pageable pageable);

    // Count by role
    long countByRole(User.Role role);

    long countByRoleAndEnabled(User.Role role, boolean enabled);
}
