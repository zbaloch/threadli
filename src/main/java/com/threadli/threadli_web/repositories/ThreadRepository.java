package com.threadli.threadli_web.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.threadli.threadli_web.models.Thread;


@Repository
public interface ThreadRepository extends JpaRepository<Thread, Long> {
    // Find threads for a user, excluding soft-deleted
    List<Thread> findByMemberships_User_IdAndIsDeletedFalseOrderByUpdatedAtDesc(Long userId);

    // Find single thread by id, excluding soft-deleted
    Optional<Thread> findByIdAndIsDeletedFalse(Long threadId);

    // Find threads by channel, excluding soft-deleted threads
    List<Thread> findByChannelIdAndIsDeletedFalseOrderByUpdatedAtDesc(Long channelId);

}
