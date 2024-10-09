package com.async.async_web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.async.async_web.models.WorkspaceMembership;
import com.async.async_web.models.User;
import com.async.async_web.models.Workspace;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, Long> {
    Optional<WorkspaceMembership> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);
}
