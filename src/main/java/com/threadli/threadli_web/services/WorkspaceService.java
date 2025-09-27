package com.threadli.threadli_web.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.threadli.threadli_web.models.User;
import com.threadli.threadli_web.models.Workspace;
import com.threadli.threadli_web.models.WorkspaceMembership;
import com.threadli.threadli_web.models.WorkspaceRole;
import com.threadli.threadli_web.repositories.WorkspaceMembershipRepository;
import com.threadli.threadli_web.repositories.WorkspaceRepository;

@Service
public class WorkspaceService {

    @Autowired
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;


}
