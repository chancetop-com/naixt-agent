package com.chancetop.naixt.agent.api.naixt;

import core.framework.api.json.Property;
import core.framework.api.validate.Size;

/**
 * @author stephen
 */
public class AgentChatRequest {
    @Size(max = 128 * 1000)
    @Property(name = "query")
    public String query;

    @Property(name = "settings")
    public NaixtPluginSettingsView settings;

    @Property(name = "edit_info")
    public CurrentEditInfoView editInfo;
}
