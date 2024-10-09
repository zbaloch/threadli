package com.async.async_web.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.async.async_web.models.Thread;

@Repository
public interface ThreadRepository extends JpaRepository<Thread, Long> {
    // Custom query methods can be added here if needed
    List<Thread> findByWorkspaceId(Long workspaceId);

    // Find by channel id and thread id 
    Optional<Thread> findByWorkspaceIdAndId(Long workspaceId, Long threadId);
}
