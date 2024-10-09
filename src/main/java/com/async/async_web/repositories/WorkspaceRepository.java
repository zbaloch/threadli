package com.async.async_web.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.async.async_web.models.Workspace;
import com.async.async_web.models.WorkspaceMembership;
import java.util.List;
import java.util.Set;


@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    // Generate method to get a workspace based on userId and workspace Id
    Optional<Workspace> findByMembershipsUserIdAndId(Long userId, Long workspaceId);
    List<Workspace> findByMembershipsUserId(Long userId);
}
