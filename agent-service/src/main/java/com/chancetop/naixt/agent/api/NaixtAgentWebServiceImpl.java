package com.chancetop.naixt.agent.api;

import com.chancetop.naixt.agent.api.naixt.ChatResponse;
import com.chancetop.naixt.agent.api.naixt.NaixtChatRequest;
import com.chancetop.naixt.agent.service.NaixtAgentService;
import core.framework.inject.Inject;

/**
 * @author stephen
 */
public class NaixtAgentWebServiceImpl implements NaixtAgentWebService {
    @Inject
    NaixtAgentService naixtAgentService;

    @Override
    public ChatResponse chat(NaixtChatRequest request) {
        return naixtAgentService.chat(request);
    }

    @Override
    public void clear() {
        naixtAgentService.clear();
    }
}
