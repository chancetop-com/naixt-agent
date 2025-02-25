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

    public enum Action {
        @Property(name = "ADD")
        ADD,
        @Property(name = "DELETE")
        DELETE,
        @Property(name = "MODIFY")
        MODIFY
    }

    public static class FileContent {
        @Property(name = "content")
        public String content;

        @Property(name = "file_path")
        public String filePath;

        @Property(name = "action")
        public Action action;
    }
}
