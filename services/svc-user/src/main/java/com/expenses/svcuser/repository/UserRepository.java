package com.expenses.svcuser.repository;

import com.expenses.svcuser.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByKeycloakUserId(String keycloakUserId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByKeycloakUserId(String keycloakUserId);

  @Query("SELECT u FROM User u WHERE u.isActive = true")
  Page<User> findAllActiveUsers(Pageable pageable);

  @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
      "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<User> searchActiveUsers(@Param("search") String search, Pageable pageable);

  @Query("SELECT u FROM User u WHERE u.id IN :userIds AND u.isActive = true")
  Page<User> findActiveUsersByIds(@Param("userIds") Iterable<UUID> userIds, Pageable pageable);
}
