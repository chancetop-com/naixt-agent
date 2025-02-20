package com.chancetop.naixt.agent.service;

import ai.core.agent.Agent;
import com.chancetop.naixt.agent.api.naixt.ChatResponse;
import com.chancetop.naixt.agent.api.naixt.NaixtChatRequest;
import ai.core.llm.providers.LiteLLMProvider;
import com.chancetop.naixt.agent.utils.IdeUtils;
import core.framework.inject.Inject;

import java.util.HashMap;

/**
 * @author stephen
 */
public class NaixtService {
    @Inject
    LiteLLMProvider liteLLMProvider;
    private Agent agent;

    public ChatResponse chat(NaixtChatRequest request) {
        if (agent == null) {
            agent = Agent.builder()
                    .systemPrompt("""
                    You are an assistant that helps users write code.
                    You have a highly skilled software engineer with extensive experience in Java, TypeScript, JavaScript and HTML/CSS.
                    You have extensive knowledge in software development principles, design patterns, and best practices.
                    """)
                        .promptTemplate("""
                    User current editor file content:
                    {{current_file_content}}
                    User current editor position:
                    line: {{current_line_number}}, column: {{current_column_number}}
                    User's query:
                    """)
                    .model(request.model)
                    .llmProvider(liteLLMProvider).build();
        }
        var context = new HashMap<String, Object>();
        context.put("current_file_content", IdeUtils.getFileContent(request.currentFilePath));
        context.put("current_line_number", request.currentLineNumber);
        context.put("current_column_number", request.currentColumnNumber);
        return ChatResponse.of(agent.run(request.query, context));
    }
}
