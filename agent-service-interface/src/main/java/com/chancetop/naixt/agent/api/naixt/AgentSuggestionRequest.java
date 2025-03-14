package com.chancetop.naixt.agent.api.naixt;

import core.framework.api.json.Property;

/**
 * @author stephen
 */
public class AgentSuggestionRequest {
    @Property(name = "settings")
    public NaixtPluginSettingsView settings;

    @Property(name = "edit_info")
    public CurrentEditInfoView editInfo;
}
