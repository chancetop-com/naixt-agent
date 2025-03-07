package com.chancetop.naixt.agent;

import com.chancetop.naixt.agent.api.NaixtAgentWebService;
import com.chancetop.naixt.agent.api.NaixtAgentWebServiceImpl;
import com.chancetop.naixt.agent.api.NaixtWebService;
import com.chancetop.naixt.agent.api.NaixtWebServiceImpl;
import com.chancetop.naixt.agent.api.naixt.ChatResponse;
import com.chancetop.naixt.agent.listener.NaixtAgentSSEListener;
import com.chancetop.naixt.agent.service.NaixtAgentService;
import com.chancetop.naixt.agent.service.NaixtService;
import com.chancetop.naixt.agent.service.WorkspaceToolingService;
import core.framework.http.HTTPMethod;
import core.framework.module.Module;
import core.framework.module.ServerSentEventConfig;

import java.time.Duration;

/**
 * @author stephen
 */
public class AgentModule extends Module {
    @Override
    protected void initialize() {
//        bind(LanguageServerToolingService.class);
        bind(WorkspaceToolingService.class);
        bind(NaixtService.class);
        bind(NaixtAgentService.class);
        api().service(NaixtWebService.class, bind(NaixtWebServiceImpl.class));
        api().service(NaixtAgentWebService.class, bind(NaixtAgentWebServiceImpl.class));
        http().limitRate().add(ServerSentEventConfig.SSE_CONNECT_GROUP, 5000, 1000, Duration.ZERO);
        sse().listen(HTTPMethod.PUT, "/naixt/agent/chat-sse", ChatResponse.class, bind(NaixtAgentSSEListener.class));
    }
}
