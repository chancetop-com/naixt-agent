package com.chancetop.naixt.agent.api;

import com.chancetop.naixt.agent.api.naixt.ApproveChangeRequest;
import com.chancetop.naixt.agent.api.naixt.ChatResponse;
import com.chancetop.naixt.agent.api.naixt.NaixtChatRequest;
import core.framework.api.web.service.PUT;
import core.framework.api.web.service.Path;

/**
 * @author stephen
 */
public interface NaixtAgentWebService {
    @PUT
    @Path("/naixt/agent/chat")
    ChatResponse chat(NaixtChatRequest request);

    @PUT
    @Path("/naixt/agent/clear")
    void clear();

    @PUT
    @Path("/naixt/agent/change-list-approved")
    void approved(ApproveChangeRequest request);
}
