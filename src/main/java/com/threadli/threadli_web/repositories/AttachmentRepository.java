package com.threadli.threadli_web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadli.threadli_web.models.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
}
