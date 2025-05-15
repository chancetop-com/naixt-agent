package com.chancetop.naixt.agent;

import com.chancetop.naixt.agent.api.NaixtAgentWebService;
import com.chancetop.naixt.agent.api.NaixtAgentWebServiceImpl;
import com.chancetop.naixt.agent.api.NaixtWebService;
import com.chancetop.naixt.agent.api.NaixtWebServiceImpl;
import com.chancetop.naixt.agent.api.naixt.AgentChatResponse;
import com.chancetop.naixt.agent.listener.NaixtAgentSSEListener;
import com.chancetop.naixt.agent.service.NaixtAgentService;
import com.chancetop.naixt.agent.service.NaixtService;
import core.framework.http.HTTPMethod;
import core.framework.module.Module;

/**
 * @author stephen
 */
public class AgentModule extends Module {
    @Override
    protected void initialize() {
//        bind(LanguageServerToolingService.class);
        bind(NaixtService.class);
        bind(NaixtAgentService.class);
        api().service(NaixtWebService.class, bind(NaixtWebServiceImpl.class));
        api().service(NaixtAgentWebService.class, bind(NaixtAgentWebServiceImpl.class));
        sse().listen(HTTPMethod.PUT, "/naixt/agent/chat-sse", AgentChatResponse.class, bind(NaixtAgentSSEListener.class));
    }
}
