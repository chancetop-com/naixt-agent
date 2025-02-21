package com.chancetop.naixt.agent.api.naixt;

import com.google.gson.annotations.SerializedName;
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

    @SerializedName("text")
    @Property(name = "text")
    public String text;
}
