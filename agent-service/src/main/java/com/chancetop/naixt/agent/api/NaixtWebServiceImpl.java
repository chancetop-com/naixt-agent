package com.chancetop.naixt.agent.api;

import com.chancetop.naixt.agent.service.NaixtService;
import core.framework.inject.Inject;

/**
 * @author stephen
 */
public class NaixtWebServiceImpl implements NaixtWebService {
    @Inject
    NaixtService naixtService;

    @Override
    public void stop() {
        naixtService.stop();
    }
}
