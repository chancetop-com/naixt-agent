package com.chancetop.naixt.agent.agent;

import ai.core.agent.Agent;
import ai.core.llm.LLMProvider;

/**
 * @author stephen
 */
public class TaskSuggestionAgent {
    public static Agent of(LLMProvider llmProvider, String model) {
        return Agent.builder()
                .name("suggestion-agent")
                .description("suggestion-agent is an agent that provides suggestion tasks for the user.")
                .systemPrompt("""
                        You are a highly skilled software engineer with extensive experience in Java, TypeScript, JavaScript and HTML/CSS.
                        You have extensive knowledge in software development principles, design patterns, and best practices.
                        Based on the user's workspace information, you need to provide 3-5 task suggestions to help users quickly start their programming work.
                        You must read the current editor file content, do not suggest tasks that no need to be done, for example, if the imports is clean, do not suggest to remove unused imports or if highlight error is empty, do not suggest to resolve the errors.
                        Output requirements:
                        - Each suggestion should be a phrase keyword of no more than 5 words.
                        - 1 line for each suggestion.
                        Example output:
                        - Remove unused imports
                        - Format the code
                        - Resolve the errors
                        - Optimize the code
                        """)
                .promptTemplate("""
                        Workspace information:
                        Workspace path: {{workspace_path}}
                        Workspace file tree: {{workspace_file_tree}}
                        User current editor file {{current_file_path}}'s content:
                        ```
                        {{current_file_content}}
                        ```
                        User current editor file's highlighted errors:
                        {{current_file_diagnostic}}
                        User current editor position: line: {{current_line_number}}, column: {{current_column_number}}
                        
                        """)
                .model(model)
                .llmProvider(llmProvider).build();
    }
}
