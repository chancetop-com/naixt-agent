package com.chancetop.naixt.agent.listener;

import com.chancetop.naixt.agent.api.naixt.ChatResponse;
import com.chancetop.naixt.agent.api.naixt.NaixtChatRequest;
import com.chancetop.naixt.agent.service.NaixtAgentService;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.web.Request;
import core.framework.web.sse.Channel;
import core.framework.web.sse.ChannelListener;

/**
 * @author stephen
 */
public class NaixtAgentSSEListener implements ChannelListener<ChatResponse> {
    @Inject
    NaixtAgentService naixtAgentService;

    @Override
    public void onConnect(Request request, Channel<ChatResponse> channel, String lastEventId) {
        naixtAgentService.chatSse(JSON.fromJSON(NaixtChatRequest.class, new String(request.body().orElseThrow())), channel);
        channel.close();
    }
}
