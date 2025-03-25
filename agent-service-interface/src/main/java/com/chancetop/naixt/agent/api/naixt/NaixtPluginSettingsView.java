package com.chancetop.naixt.agent.api.naixt;

import core.framework.api.json.Property;

/**
 * @author stephen
 */
public class NaixtPluginSettingsView {
    @Property(name = "model")
    public String model;

    @Property(name = "planning_model")
    public String planningModel;

    @Property(name = "atlassian_enabled")
    public Boolean atlassianEnabled;

    @Property(name = "atlassian_mcp_setting")
    public NaixtPluginMcpSettingView atlassianMcpSetting;
}
