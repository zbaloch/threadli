package com.threadli.threadli_web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.threadli.threadli_web.models.ThreadMembership;
import com.threadli.threadli_web.models.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThreadMembershipRepository extends JpaRepository<ThreadMembership, Long> {
    ThreadMembership findByUserIdAndThreadId(Long userId, Long threadId);
    List<ThreadMembership> findByThreadId(Long threadId);
    Optional<ThreadMembership> findByThreadIdAndUserId(Long threadId, Long userId);
    
    @Query("SELECT DISTINCT tm.user FROM thread_members tm WHERE tm.caughtUp = false")
    List<User> findUsersWithUncaughtUpThreads();

}
