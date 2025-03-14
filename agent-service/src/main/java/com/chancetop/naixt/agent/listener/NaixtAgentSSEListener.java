package com.chancetop.naixt.agent.listener;

import com.chancetop.naixt.agent.api.naixt.AgentChatResponse;
import com.chancetop.naixt.agent.api.naixt.AgentChatRequest;
import com.chancetop.naixt.agent.service.NaixtAgentService;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.web.Request;
import core.framework.web.sse.Channel;
import core.framework.web.sse.ChannelListener;

/**
 * @author stephen
 */
public class NaixtAgentSSEListener implements ChannelListener<AgentChatResponse> {
    @Inject
    NaixtAgentService naixtAgentService;

    @Override
    public void onConnect(Request request, Channel<AgentChatResponse> channel, String lastEventId) {
        naixtAgentService.chatSse(JSON.fromJSON(AgentChatRequest.class, new String(request.body().orElseThrow())), channel);
        channel.close();
    }
}
