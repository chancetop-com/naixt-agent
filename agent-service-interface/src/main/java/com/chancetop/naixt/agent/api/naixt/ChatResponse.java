package com.chancetop.naixt.agent.api.naixt;

import core.framework.api.json.Property;

import java.util.List;

/**
 * @author stephen
 */
public class ChatResponse {
    public static ChatResponse of(String text) {
        ChatResponse response = new ChatResponse();
        response.text = text;
        return response;
    }

    @Property(name = "planning")
    public String text;

    @Property(name = "file_contents")
    public List<FileContent> fileContents;
}
