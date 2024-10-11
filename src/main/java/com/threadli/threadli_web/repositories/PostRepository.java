package com.threadli.threadli_web.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.threadli.threadli_web.models.Post;
import com.threadli.threadli_web.models.User;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // Custom query methods can be added here if needed
    List<Post> findByThreadId(Long threadId);

    // Find by thread id and post id 
    Optional<Post> findByThreadIdAndId(Long threadId, Long postId);
    Optional<Post> findByCreatedByAndId(User createdBy, Long postId);

    Optional<Post> findById(Long id);
}
