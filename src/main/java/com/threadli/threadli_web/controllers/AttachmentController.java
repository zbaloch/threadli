package com.threadli.threadli_web.controllers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.threadli.threadli_web.models.Attachment;
import com.threadli.threadli_web.services.AttachmentService;

@Controller
public class AttachmentController {
    private static final Logger log = LoggerFactory.getLogger(AttachmentController.class);

    @Autowired
    private AttachmentService attachmentService;

    @PostMapping("/upload/{workspaceId}")
    @ResponseBody
    public String uploadAttachment(@PathVariable Long workspaceId, 
        @RequestParam("file") MultipartFile file) {
        try {
            Attachment attachment = attachmentService.saveAttachment(file, workspaceId);
            return "/upload/" + attachment.getId() + "/" + attachment.getFileName();
        } catch (IOException e) {
            return "Failed to upload file: " + e.getMessage();
        }
    }

    @GetMapping("/upload/{attachmentId}/{fileName}")
    public ResponseEntity<byte[]> getAttachment(@PathVariable Long attachmentId, 
        @PathVariable String fileName) {
        Attachment attachment = attachmentService.findById(attachmentId);

        if (attachment == null || attachment.getData() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, attachment.getFileType());
        return new ResponseEntity<>(attachment.getData(), headers, HttpStatus.OK);
    }
    
}
