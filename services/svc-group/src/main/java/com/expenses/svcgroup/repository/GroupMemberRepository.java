package com.expenses.svcgroup.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expenses.svcgroup.entity.GroupMember;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

  @Query("SELECT m FROM GroupMember m WHERE m.group.id = :groupId AND m.status = 'ACTIVE'")
  List<GroupMember> findActiveByGroupId(@Param("groupId") UUID groupId);

  @Query("SELECT m FROM GroupMember m WHERE m.userId = :userId AND m.status = 'ACTIVE'")
  List<GroupMember> findActiveByUserId(@Param("userId") UUID userId);

  @Query("SELECT m FROM GroupMember m WHERE m.group.id = :groupId AND m.userId = :userId")
  Optional<GroupMember> findByGroupIdAndUserId(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

  @Query("SELECT m FROM GroupMember m WHERE m.group.id = :groupId AND m.role = 'OWNER' AND m.status = 'ACTIVE'")
  Optional<GroupMember> findOwnerByGroupId(@Param("groupId") UUID groupId);

  @Query("SELECT m FROM GroupMember m WHERE m.group.id = :groupId AND m.role IN ('OWNER', 'ADMIN') AND m.status = 'ACTIVE'")
  List<GroupMember> findAdminsByGroupId(@Param("groupId") UUID groupId);

  @Query("SELECT COUNT(m) FROM GroupMember m WHERE m.group.id = :groupId AND m.status = 'ACTIVE'")
  long countActiveByGroupId(@Param("groupId") UUID groupId);

  @Query("SELECT COUNT(m) FROM GroupMember m WHERE m.userId = :userId AND m.status = 'ACTIVE'")
  long countActiveByUserId(@Param("userId") UUID userId);

  boolean existsByGroupIdAndUserIdAndStatus(UUID groupId, UUID userId, GroupMember.MemberStatus status);

  @Query("SELECT m FROM GroupMember m WHERE m.group.id = :groupId AND m.role = :role AND m.status = 'ACTIVE'")
  List<GroupMember> findByGroupIdAndRole(@Param("groupId") UUID groupId, @Param("role") GroupMember.MemberRole role);

  @Query("SELECT m FROM GroupMember m WHERE m.invitedBy = :inviterId AND m.status = 'INVITED'")
  List<GroupMember> findPendingInvitationsByInviter(@Param("inviterId") UUID inviterId);
}
