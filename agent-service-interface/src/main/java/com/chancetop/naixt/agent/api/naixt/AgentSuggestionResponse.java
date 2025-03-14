package com.chancetop.naixt.agent.api.naixt;

import core.framework.api.json.Property;

import java.util.List;

/**
 * @author stephen
 */
public class AgentSuggestionResponse {
    public static AgentSuggestionResponse of(List<String> suggestions) {
        var rsp = new AgentSuggestionResponse();
        rsp.suggestions = suggestions;
        return rsp;
    }

    @Property(name = "suggestions")
    public List<String> suggestions;
}
