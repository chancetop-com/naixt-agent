package com.chancetop.naixt.agent.api;

import core.framework.api.web.service.PUT;
import core.framework.api.web.service.Path;

/**
 * @author stephen
 */
public interface NaixtWebService {
    @PUT
    @Path("/naixt/stop")
    void stop();
}
