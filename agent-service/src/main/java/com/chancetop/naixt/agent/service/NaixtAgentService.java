package com.chancetop.naixt.agent.service;

import ai.core.agent.Agent;
import ai.core.agent.formatter.formatters.DefaultJsonFormatter;
import com.chancetop.naixt.agent.api.naixt.ChatResponse;
import com.chancetop.naixt.agent.api.naixt.NaixtChatRequest;
import ai.core.llm.providers.LiteLLMProvider;
import com.chancetop.naixt.agent.utils.IdeUtils;
import core.framework.inject.Inject;
import core.framework.json.JSON;

import java.util.HashMap;

/**
 * @author stephen
 */
public class NaixtAgentService {
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
                    You have a strong understanding of CoreNG, CoreFE, CoreAI framework.
                    Output requirements:
                    - The entire output should be in JSON format.
                    - The output should contain the following keys: "planning", "partial", "file_contents".
                    - Place your planning text in the "planning" key.
                    - The "partial" key should contain a boolean value indicating whether the task is partial or complete at this time.
                    - The "file_contents" key should contain a list of objects, each with the keys "file_path", "content", and "action".
                    - The action value should be one of "ADD", "DELETE", or "MODIFY".
                    - The content value should be empty for "DELETE" action.
                    The output json example:
                    {
                      "planning": "I will add a new import statement to the ExampleService.java file.",
                      "file_contents": [
                        {
                          "file_path": "src/main/java/org/example/server/ExampleService.java",
                          "content": "java source code",
                          "action": "MODIFY"
                        }
                      ],
                      "partial": false
                    }
                    Other requirements:
                    Your need to analyze the user's query.
                    If the user hopes for code generation, then generate the code according to the Output requirements.
                    If it is not code-related, still include your response in the "planning" key.
                    """)
                    .promptTemplate("""
                    Workspace file tree:
                    {{workspace_file_tree}}
                    User current editor file {{current_file_path}}'s content:
                    {{current_file_content}}
                    User current editor position:
                    line: {{current_line_number}}, column: {{current_column_number}}
                    User's query:
                    """)
                    .model(request.model)
                    .formatter(new DefaultJsonFormatter())
                    .llmProvider(liteLLMProvider).build();
        }
        var context = new HashMap<String, Object>();
        context.put("current_file_path", IdeUtils.toWorkspaceRelativePath(request.workspacePath, request.currentFilePath));
        context.put("current_file_content", IdeUtils.getFileContent(request.currentFilePath));
        context.put("current_line_number", request.currentLineNumber);
        context.put("current_column_number", request.currentColumnNumber);
        var rsp = agent.run(request.query, context);
        return JSON.fromJSON(ChatResponse.class, rsp);
    }

    public void clear() {
        if (agent == null) return;
        agent.clearShortTermMemory();
    }
}
