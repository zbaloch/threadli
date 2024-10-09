package com.async.async_web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.async.async_web.models.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
}
