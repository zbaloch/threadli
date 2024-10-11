package com.threadli.threadli_web.services;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.threadli.threadli_web.models.Attachment;
import com.threadli.threadli_web.models.Workspace;
import com.threadli.threadli_web.repositories.AttachmentRepository;
import com.threadli.threadli_web.repositories.WorkspaceRepository;


@Service
public class AttachmentService {
    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    public Attachment saveAttachment(MultipartFile file, Long workspaceId) throws IOException {

        Attachment attachment = new Attachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setData(file.getBytes());
        // Assuming you have a method to get the Chapter by ID
        // Post post = postRepository.findById(postId).get();
        // attachment.setPost(post);
        Workspace workspace = workspaceRepository.findById(workspaceId).get();
        attachment.setWorkspace(workspace);

        return attachmentRepository.save(attachment);
    }

    public Attachment findById(Long attacmentId) {
        return attachmentRepository.findById(attacmentId).get();
    }

}
