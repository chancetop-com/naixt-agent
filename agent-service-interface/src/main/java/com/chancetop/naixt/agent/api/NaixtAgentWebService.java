package com.chancetop.naixt.agent.api;

import com.chancetop.naixt.agent.api.naixt.AgentApproveRequest;
import com.chancetop.naixt.agent.api.naixt.AgentSuggestionRequest;
import com.chancetop.naixt.agent.api.naixt.AgentSuggestionResponse;
import com.chancetop.naixt.agent.api.naixt.AgentChatResponse;
import com.chancetop.naixt.agent.api.naixt.AgentChatRequest;
import core.framework.api.web.service.PUT;
import core.framework.api.web.service.Path;

/**
 * @author stephen
 */
public interface NaixtAgentWebService {
    @PUT
    @Path("/naixt/agent/chat")
    AgentChatResponse chat(AgentChatRequest request);

    @PUT
    @Path("/naixt/agent/suggestion")
    AgentSuggestionResponse suggestion(AgentSuggestionRequest request);

    @PUT
    @Path("/naixt/agent/clear")
    void clear();

    @PUT
    @Path("/naixt/agent/change-list-approved")
    void approved(AgentApproveRequest request);
}
