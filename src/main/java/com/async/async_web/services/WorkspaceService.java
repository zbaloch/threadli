package com.async.async_web.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.async.async_web.models.User;
import com.async.async_web.models.Workspace;
import com.async.async_web.models.WorkspaceMembership;
import com.async.async_web.models.WorkspaceRole;
import com.async.async_web.repositories.WorkspaceMembershipRepository;
import com.async.async_web.repositories.WorkspaceRepository;

@Service
public class WorkspaceService {

    @Autowired
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;


}
