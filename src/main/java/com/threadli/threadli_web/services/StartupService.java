package com.threadli.threadli_web.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.threadli.threadli_web.models.Workspace;
import com.threadli.threadli_web.repositories.WorkspaceRepository;

import jakarta.annotation.PostConstruct;

@Service
public class StartupService {

    private static final Logger log = LoggerFactory.getLogger(StartupService.class);

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @PostConstruct
    public void init() {
        createDefaultWorkspaceIfNotExists();
    }

    private void createDefaultWorkspaceIfNotExists() {
        // Check if any workspace exists
        long workspaceCount = workspaceRepository.count();
        
        if (workspaceCount == 0) {
            log.info("No workspaces found. Creating default workspace...");
            
            Workspace defaultWorkspace = new Workspace();
            defaultWorkspace.setName("Default Workspace");
            // Note: createdBy is null for the default workspace since no user exists yet
            defaultWorkspace.setCreatedBy(null);
            
            workspaceRepository.save(defaultWorkspace);
            
            log.info("Default workspace created successfully with ID: {}", defaultWorkspace.getId());
        } else {
            log.info("Workspaces already exist ({}). Skipping default workspace creation.", workspaceCount);
        }
    }
}