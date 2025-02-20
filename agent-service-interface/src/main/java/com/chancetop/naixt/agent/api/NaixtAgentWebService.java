package com.chancetop.naixt.agent.api;

import com.chancetop.naixt.agent.api.naixt.ChatResponse;
import com.chancetop.naixt.agent.api.naixt.NaixtChatRequest;
import core.framework.api.web.service.PUT;
import core.framework.api.web.service.Path;

/**
 * @author stephen
 */
public interface NaixtAgentWebService {
    @PUT
    @Path("/naixt/chat")
    ChatResponse chat(NaixtChatRequest request);
}
