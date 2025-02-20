package com.chancetop.naixt.agent;

import com.chancetop.naixt.agent.api.NaixtAgentWebService;
import com.chancetop.naixt.agent.api.NaixtAgentWebServiceImpl;
import com.chancetop.naixt.agent.service.NaixtService;
import core.framework.module.Module;

/**
 * @author stephen
 */
public class AgentModule extends Module {
    @Override
    protected void initialize() {
//        bind(LanguageServerToolingService.class);
        bind(NaixtService.class);
        api().service(NaixtAgentWebService.class, bind(NaixtAgentWebServiceImpl.class));
    }
}
