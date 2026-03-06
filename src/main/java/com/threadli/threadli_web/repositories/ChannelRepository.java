package com.threadli.threadli_web.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.threadli.threadli_web.models.Channel;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findAllByOrderByNameAsc();

    Optional<Channel> findBySlug(String slug);
}
