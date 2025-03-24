package com.chancetop.naixt.agent.agent;

import ai.core.agent.Agent;
import ai.core.agent.AgentGroup;
import ai.core.agent.Node;
import ai.core.agent.formatter.formatters.DefaultJsonFormatter;
import ai.core.llm.LLMProvider;
import ai.core.persistence.PersistenceProvider;
import ai.core.tool.function.Functions;
import com.chancetop.naixt.agent.api.naixt.FileContent;
import com.chancetop.naixt.agent.service.WorkspaceToolingService;
import core.framework.api.json.Property;
import core.framework.api.validate.NotNull;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author stephen
 */
public class CodingAgentGroup {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodingAgentGroup.class);

    public static Agent moderatorAgent(LLMProvider llmProvider, String goal, List<Node<?>> agents, String model) {
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
                        Please generate the detailed query for the next step, including all necessary context.
                        You only do the planning and choose the next agent to play, do not execute any task for example code generating.
                        Think in the user's language.
                        Read the conversation and think if we already finish the task, if yes, TERMINATE, if not, choose the next agent to play.
                        If you think we already finish the task, please return the "next_step" valued: TERMINATE and leave the "name" empty.
                        If you sure that the next agent is the last one and it can finish the task, please return the "next_step" valued: TERMINATE and place the agent name in the key "name".
                        If you are re-planning, please include the reason information in the query for the next agent.
                        Return a json that contain your planning steps and current step and the agent's name and a string query generated for the selected agent, for example:
                        {"planning": "1. step1; 2. step2", "next_step": "TERMINATE", "name": "order-expert-agent", "query": "list the user's most recent orders"}.
                        Only return the json, do not print anything else.
                        Always remember the goal and the agents list.
                        """, goal, AgentGroup.AgentsInfo.agentsInfo(agents)))
                .promptTemplate("""
                        Workspace path: {{workspace_path}}
                        Workspace file tree: {{workspace_file_tree}}
                        User current editor file {{current_file_path}}'s content:
                        ```
                        {{current_file_content}}
                        ```
                        User current editor position: line: {{current_line_number}}, column: {{current_column_number}}
                        User current editor file's highlighted errors:
                        {{current_file_diagnostic}}
                        Previous agent output/raw input:
                        """)
                .formatter(new DefaultJsonFormatter(true))
                .model(model)
                .llmProvider(llmProvider).build();
    }
    public static Agent codingAgent(LLMProvider llmProvider, String model) {
        return Agent.builder().name("coding-agent")
                .description("coding-agent is an agent that helps users write code.")
                .systemPrompt("""
                    You are an assistant that helps users write code.
                    You are a highly skilled software engineer with extensive experience in Java, TypeScript, JavaScript and HTML/CSS.
                    You have extensive knowledge in software development principles, design patterns, and best practices.
                    You have a strong understanding of CoreNG, CoreFE, CoreAI framework.
                    You must use your expertise to provide the user with the best possible solution to their coding requirements.
                    You need to read the conversation if the file content you need already provided by the workspace-agent or is user's current edit file.
                    You need to analysis the user's query to choose the correct files to modify, don't always assume that the user's requirement can be solved by modifying the current file.
                    Output requirements:
                    - The entire output should be in JSON format.
                    - The output should contain the following keys: "description", "partial", "file_contents".
                    - Place your description text in the "description" key.
                    - The "partial" key should contain a boolean value indicating whether the task is partial or complete at this time.
                    - The "file_contents" key should contain a list of objects, each with the keys "file_path", "content", and "action".
                    - Place the code in the "content" key, the content should be the whole file content after the modification.
                    - The action value should be one of "ADD", "DELETE", or "MODIFY".
                    - The content value should be empty for "DELETE" action.
                    - Description use the user's language.
                    The output json example:
                    {
                      "description": "I will add a new import statement to the ExampleService.java file, and add module declaration in the build.gradle file.",
                      "file_contents": [
                        {
                          "file_path": "src/main/java/org/example/server/ExampleService.java",
                          "content": "java source code",
                          "action": "MODIFY"
                        },
                        {
                          "file_path": "other file path",
                          "content": "other file content",
                          "action": "ADD"
                        }
                      ],
                      "partial": false
                    }
                    Other requirements:
                    Your need to analyze the user's query, you should not always assume that the user wants to generate code.
                    If the user hopes for code generation, then generate the code according to the Output requirements.
                    If it is not code-related, still include your response in the "description" key.
                    Please make sure that the code you write is consistent with the existing code style and framework of the project.
                    """)
                .promptTemplate("""
                    Workspace path: {{workspace_path}}
                    Workspace file tree: {{workspace_file_tree}}
                    User current editor file {{current_file_path}}'s content:
                    ```
                    {{current_file_content}}
                    ```
                    User current editor file's highlighted errors:
                    {{current_file_diagnostic}}
                    User current editor position: line: {{current_line_number}}, column: {{current_column_number}}
                    User's query:
                    """)
                .model(model)
                .formatter(new DefaultJsonFormatter(true))
                .llmProvider(llmProvider).build();
    }

    public static AgentGroup of(LLMProvider llmProvider, PersistenceProvider persistenceProvider, String model, String planningModel, String vectorStorePath) {
        var codingAgent = codingAgent(llmProvider, model);
        var workspaceAgent = Agent.builder()
                .name("workspace-agent")
                .description("workspace-agent is an agent that provide project's workspace information for coding.")
                .systemPrompt("""
                        You are an assistant that provide the project's workspace information for coding.
                        For example, you can provide the workspace path, file tree, and the content of a file.
                        When using the 'get file tree' tool, use the 'recursive' parameter cautiously. Unless your query explicitly tells you or you believe it is necessary, please set this parameter to false.
                        If the file tree text is too long to handle, please get file tree layer by layer according to the directory hierarchy.
                        """)
                .toolCalls(Functions.from(new WorkspaceToolingService()))
                .promptTemplate("""
                        Workspace path: {{workspace_path}}
                        query:
                        """)
                .llmProvider(llmProvider).build();
        List<Node<?>> agents = List.of(codingAgent, workspaceAgent);
        var goal = """
                coding-agent-group is a group of agents that help user to write code.
                We only need to focus on generating code, no need to confirm the modification or verify the content.
                We need to analysis the user's query to choose the correct files to modify, don't always assume that the user's requirement can be solved by modifying the current file.
                Ask the workspace-agent if we need more information about the workspace like file tree or file content.
                Read the conversation if the file content already provided by the workspace-agent or is user's current edit file.
                We need to carefully analyze the requirements and try to modify or add files according to the existing code structure, rather than directly starting to add new files.
                Make sure the coding-agent is the last agent to play.
                """;
        LOGGER.info(vectorStorePath);
//        var vectorStore = new HnswLibVectorStore(HnswConfig.of(vectorStorePath));
//        var longQueryHandler = new LongQueryHandler(llmProvider, LongQueryHandlerType.RAG, vectorStore, new RecursiveCharacterTextSplitter());
        return AgentGroup.builder()
                .name("coding-agent-group")
                .description(goal)
                .agents(agents)
                .moderator(moderatorAgent(llmProvider, goal, agents, planningModel))
                .persistenceProvider(persistenceProvider)
                .maxRound(8)
                .llmProvider(llmProvider).build();
    }

    public static class CodingResponse {
        public static CodingResponse of(String text) {
            var response = new CodingResponse();
            response.text = text;
            return response;
        }

        @NotNull
        @Property(name = "description")
        public String text;

        @NotNull
        @Property(name = "file_contents")
        public List<FileContent> fileContents = List.of();
    }
}
