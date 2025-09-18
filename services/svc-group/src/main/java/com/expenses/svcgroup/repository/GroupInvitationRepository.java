package com.expenses.svcgroup.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expenses.svcgroup.entity.GroupInvitation;

@Repository
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, UUID> {

  Optional<GroupInvitation> findByInvitationCode(String invitationCode);

  @Query("SELECT i FROM GroupInvitation i WHERE i.group.id = :groupId AND i.status = 'PENDING' AND i.expiresAt > :now")
  List<GroupInvitation> findActivePendingByGroupId(@Param("groupId") UUID groupId, @Param("now") ZonedDateTime now);

  @Query("SELECT i FROM GroupInvitation i WHERE i.invitedEmail = :email AND i.status = 'PENDING' AND i.expiresAt > :now")
  List<GroupInvitation> findActivePendingByEmail(@Param("email") String email, @Param("now") ZonedDateTime now);

  @Query("SELECT i FROM GroupInvitation i WHERE i.invitedBy = :userId")
  List<GroupInvitation> findByInvitedBy(@Param("userId") UUID userId);

  @Query("SELECT i FROM GroupInvitation i WHERE i.group.id = :groupId AND i.invitedEmail = :email")
  List<GroupInvitation> findByGroupIdAndEmail(@Param("groupId") UUID groupId, @Param("email") String email);

  @Query("SELECT COUNT(i) FROM GroupInvitation i WHERE i.group.id = :groupId AND i.status = 'PENDING' AND i.expiresAt > :now")
  long countActivePendingByGroupId(@Param("groupId") UUID groupId, @Param("now") ZonedDateTime now);

  @Query("SELECT i FROM GroupInvitation i WHERE i.expiresAt < :now AND i.status = 'PENDING'")
  List<GroupInvitation> findExpiredPendingInvitations(@Param("now") ZonedDateTime now);

  @Modifying
  @Query("UPDATE GroupInvitation i SET i.status = 'EXPIRED' WHERE i.expiresAt < :now AND i.status = 'PENDING'")
  int expireInvitations(@Param("now") ZonedDateTime now);

  boolean existsByGroupIdAndInvitedEmailAndStatus(UUID groupId, String invitedEmail,
      GroupInvitation.InvitationStatus status);

  @Query("SELECT i FROM GroupInvitation i WHERE i.invitedUserId = :userId AND i.status = 'PENDING' AND i.expiresAt > :now")
  List<GroupInvitation> findActivePendingByInvitedUserId(@Param("userId") UUID userId, @Param("now") ZonedDateTime now);
}
