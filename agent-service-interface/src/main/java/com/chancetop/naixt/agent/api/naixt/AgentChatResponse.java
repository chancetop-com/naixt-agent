package com.chancetop.naixt.agent.api.naixt;

import core.framework.api.json.Property;
import core.framework.api.validate.NotNull;

import java.util.List;

/**
 * @author stephen
 */
public class AgentChatResponse {
    public static AgentChatResponse of(String text) {
        AgentChatResponse response = new AgentChatResponse();
        response.text = text;
        return response;
    }

    @NotNull
    @Property(name = "planning")
    public String text;

    @NotNull
    @Property(name = "file_contents")
    public List<FileContent> fileContents = List.of();

    @NotNull
    @Property(name = "group_finished")
    public Boolean groupFinished = false;
}
