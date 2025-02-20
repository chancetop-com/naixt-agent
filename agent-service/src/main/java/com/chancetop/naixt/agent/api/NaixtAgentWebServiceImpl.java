package com.chancetop.naixt.agent.api;

import com.chancetop.naixt.agent.api.naixt.ChatResponse;
import com.chancetop.naixt.agent.api.naixt.NaixtChatRequest;
import com.chancetop.naixt.agent.service.NaixtService;
import core.framework.inject.Inject;

/**
 * @author stephen
 */
public class NaixtAgentWebServiceImpl implements NaixtAgentWebService {
    @Inject
    NaixtService naixtService;

    @Override
    public ChatResponse chat(NaixtChatRequest request) {
        return naixtService.chat(request);
    }
}
