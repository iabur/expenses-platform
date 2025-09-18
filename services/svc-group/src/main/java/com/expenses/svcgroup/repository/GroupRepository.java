package com.expenses.svcgroup.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expenses.svcgroup.entity.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

  @Query("SELECT g FROM Group g WHERE g.isActive = true")
  Page<Group> findAllActiveGroups(Pageable pageable);

  @Query("SELECT g FROM Group g WHERE g.isActive = true AND " +
      "(LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "LOWER(g.description) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<Group> searchActiveGroups(@Param("search") String search, Pageable pageable);

  @Query("SELECT g FROM Group g JOIN g.members m WHERE m.userId = :userId AND m.status = 'ACTIVE' AND g.isActive = true")
  Page<Group> findGroupsByUserId(@Param("userId") UUID userId, Pageable pageable);

  @Query("SELECT g FROM Group g JOIN g.members m WHERE m.userId = :userId AND m.role IN ('OWNER', 'ADMIN') AND m.status = 'ACTIVE' AND g.isActive = true")
  List<Group> findGroupsWhereUserIsAdmin(@Param("userId") UUID userId);

  @Query("SELECT g FROM Group g WHERE g.createdBy = :userId AND g.isActive = true")
  List<Group> findGroupsCreatedBy(@Param("userId") UUID userId);

  @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Group g JOIN g.members m " +
      "WHERE g.id = :groupId AND m.userId = :userId AND m.status = 'ACTIVE'")
  boolean isUserMemberOfGroup(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

  @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Group g JOIN g.members m " +
      "WHERE g.id = :groupId AND m.userId = :userId AND m.role IN ('OWNER', 'ADMIN') AND m.status = 'ACTIVE'")
  boolean isUserAdminOfGroup(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

  @Query("SELECT g FROM Group g WHERE g.id = :groupId AND g.isActive = true")
  Optional<Group> findActiveById(@Param("groupId") UUID groupId);

  @Query("SELECT COUNT(m) FROM Group g JOIN g.members m WHERE g.id = :groupId AND m.status = 'ACTIVE'")
  long countActiveMembersByGroupId(@Param("groupId") UUID groupId);

  List<Group> findByDefaultCurrency(String currency);

  @Query("SELECT g FROM Group g WHERE g.type = :type AND g.isActive = true")
  List<Group> findByType(@Param("type") Group.GroupType type);
}
