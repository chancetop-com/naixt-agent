package com.chancetop.naixt.agent.api.naixt;

import core.framework.api.json.Property;

/**
 * @author stephen
 */
public class ChatResponse {
    public static ChatResponse of(String text) {
        ChatResponse response = new ChatResponse();
        response.text = text;
        return response;
    }

    @Property(name = "text")
    public String text;
}
