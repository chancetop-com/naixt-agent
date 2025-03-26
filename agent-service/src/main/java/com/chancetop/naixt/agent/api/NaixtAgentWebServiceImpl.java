package com.chancetop.naixt.agent.api;

import com.chancetop.naixt.agent.api.naixt.AgentApproveRequest;
import com.chancetop.naixt.agent.api.naixt.AgentSuggestionRequest;
import com.chancetop.naixt.agent.api.naixt.AgentChatResponse;
import com.chancetop.naixt.agent.api.naixt.AgentChatRequest;
import com.chancetop.naixt.agent.api.naixt.AgentSuggestionResponse;
import com.chancetop.naixt.agent.service.NaixtAgentService;
import core.framework.inject.Inject;

/**
 * @author stephen
 */
public class NaixtAgentWebServiceImpl implements NaixtAgentWebService {
    @Inject
    NaixtAgentService naixtAgentService;

    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        return naixtAgentService.chat(request, null);
    }

    @Override
    public AgentSuggestionResponse suggestion(AgentSuggestionRequest request) {
        return naixtAgentService.suggestion(request);
    }

    @Override
    public void clear() {
        naixtAgentService.clear();
    }

    @Override
    public void approved(AgentApproveRequest request) {
        naixtAgentService.approved(request);
    }
}
