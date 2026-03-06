package com.threadli.threadli_web.services;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.threadli.threadli_web.models.Attachment;
import com.threadli.threadli_web.repositories.AttachmentRepository;

@Service
public class AttachmentService {
    @Autowired
    private AttachmentRepository attachmentRepository;

    public Attachment saveAttachment(MultipartFile file) throws IOException {
        Attachment attachment = new Attachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setData(file.getBytes());

        return attachmentRepository.save(attachment);
    }

    public Attachment findById(Long attacmentId) {
        return attachmentRepository.findById(attacmentId).get();
    }

}
