package com.chancetop.naixt.agent.agent;

import ai.core.agent.Agent;
import ai.core.agent.AgentGroup;
import ai.core.agent.Node;
import ai.core.agent.formatter.formatters.DefaultJsonFormatter;
import ai.core.llm.LLMProvider;
import ai.core.persistence.PersistenceProvider;
import ai.core.tool.function.Functions;
import com.chancetop.naixt.agent.service.WorkspaceToolingService;
import core.framework.util.Strings;

import java.util.List;

/**
 * @author stephen
 */
public class CodingAgentGroup {
    public static Agent moderatorAgent(LLMProvider llmProvider, String goal, List<Node<?>> agents) {
        return Agent.builder()
                .name("moderator-agent")
                .description("moderator of a role play game to solve task by guide the conversation and choose the next speaker agent")
                .systemPrompt(Strings.format("""
                        You are in a role play game to solve task by guide the conversation and choose the next speaker agent, the goal is:
                        {}.
                        The following agents list are available:
                        {}.
                        You need carefully review the capabilities of each agent, including the inputs and outputs of their functions to planning the conversation and choose the next agent to play.
                        Read the conversation, then select the next agent from the agents list to play.
                        Return a json that contain the agent's name and a query generated for the selected agent.
                        Read the conversation. Then select the next agent from the agents list to play.
                        Please generate the detailed query for the next step, including all necessary context.
                        If you think we already finish the task, please return the next_step valued: TERMINATE.
                        Return a json that contain your planning steps and current step and the agent's name and a string query generated for the selected agent, for example:
                        {"planning": "1. step1; 2. step2", "next_step": "step2", "name": "order-expert-agent", "query": "list the user's most recent orders"}.
                        Only return the json, do not print anything else.
                        """, goal, AgentGroup.AgentsInfo.agentsInfo(agents)))
                .promptTemplate("""
                        Workspace path: {{workspace_path}}
                        Workspace file tree: {{workspace_file_tree}}
                        User current editor file {{current_file_path}}'s content:
                        ```
                        {{current_file_content}}
                        ```
                        User current editor position: line: {{current_line_number}}, column: {{current_column_number}}
                        Previous agent output/raw input:
                        """)
                .formatter(new DefaultJsonFormatter())
                .llmProvider(llmProvider).build();
    }

    public static Agent codingAgent(LLMProvider llmProvider, String model) {
        return Agent.builder()
                .name("coding-agent")
                .description("coding-agent is an agent that helps users write code.")
                .systemPrompt("""
                    You are an assistant that helps users write code.
                    You are a highly skilled software engineer with extensive experience in Java, TypeScript, JavaScript and HTML/CSS.
                    You have extensive knowledge in software development principles, design patterns, and best practices.
                    You have a strong understanding of CoreNG, CoreFE, CoreAI framework.
                    You must use your expertise to provide the user with the best possible solution to their coding requirements.
                    Output requirements:
                    - The entire output should be in JSON format.
                    - The output should contain the following keys: "planning", "partial", "file_contents".
                    - Place your planning text in the "planning" key.
                    - The "partial" key should contain a boolean value indicating whether the task is partial or complete at this time.
                    - The "file_contents" key should contain a list of objects, each with the keys "file_path", "content", and "action".
                    - The action value should be one of "ADD", "DELETE", or "MODIFY".
                    - The content value should be empty for "DELETE" action.
                    - Remember to add a backslash before the quotes in the code of the content section.
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
                    Your need to analyze the user's query, you should not always assume that the user wants to generate code.
                    If the user hopes for code generation, then generate the code according to the Output requirements.
                    If it is not code-related, still include your response in the "planning" key.
                    """)
                .promptTemplate("""
                    Workspace path: {{workspace_path}}
                    Workspace file tree: {{workspace_file_tree}}
                    User current editor file {{current_file_path}}'s content:
                    ```
                    {{current_file_content}}
                    ```
                    User current editor position: line: {{current_line_number}}, column: {{current_column_number}}
                    User's query:
                    """)
                .model(model)
                .formatter(new DefaultJsonFormatter())
                .llmProvider(llmProvider).build();
    }

    public static AgentGroup of(LLMProvider llmProvider, PersistenceProvider persistenceProvider, String model) {
        var codingAgent = codingAgent(llmProvider, model);
        var workspaceAgent = Agent.builder()
                .name("workspace-agent")
                .description("workspace-agent is an agent that provide project's workspace information for coding.")
                .systemPrompt("You are an assistant that provide the project's workspace information for coding.")
                .toolCalls(Functions.from(new WorkspaceToolingService()))
                .promptTemplate("""
                        Workspace path: {{workspace_path}}
                        query:
                        """)
                .llmProvider(llmProvider).build();
        List<Node<?>> agents = List.of(codingAgent, workspaceAgent);
        var goal = "coding-agent-group is a group of agents that help user to write code.";
        return AgentGroup.builder()
                .name("coding-agent-group")
                .description(goal)
                .agents(agents)
                .moderator(moderatorAgent(llmProvider, goal, agents))
                .maxRound(3)
                .persistenceProvider(persistenceProvider)
                .llmProvider(llmProvider).build();
    }
}
