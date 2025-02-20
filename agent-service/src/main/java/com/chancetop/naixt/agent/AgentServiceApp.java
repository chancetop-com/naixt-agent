package com.chancetop.naixt.agent;

import ai.core.MultiAgentModule;
import com.chancetop.naixt.agent.controller.StopServiceController;
import ai.core.lsp.LanguageServerModule;
import core.framework.http.HTTPMethod;
import core.framework.module.App;
import core.framework.module.SystemModule;

/**
 * @author stephen
 */
public class AgentServiceApp extends App {
    @Override
    protected void initialize() {
        load(new SystemModule("sys.properties"));
        load(new MultiAgentModule());
        load(new LanguageServerModule());
        load(new AgentModule());
        http().route(HTTPMethod.PUT, "/_app/stop-service", bind(StopServiceController.class));
    }
}
