package com.chancetop.naixt.agent.agent;

import ai.core.agent.Agent;
import ai.core.agent.AgentGroup;
import ai.core.agent.Node;
import ai.core.agent.formatter.formatters.DefaultJsonFormatter;
import ai.core.defaultagents.DefaultModeratorAgent;
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
    public static final String CODING_CONTEXT_VARIABLE_TEMPLATE = """
            Workspace context, null or empty if not provided:
            Workspace path: {{workspace_path}}
            Workspace file tree: {{workspace_file_tree}}
            User current editor file {{current_file_path}}'s content:
            ```
            {{current_file_content}}
            ```
            User current editor position: line: {{current_line_number}}, column: {{current_column_number}}
            User current editor file's highlighted errors:
            {{current_file_diagnostic}}
            """;

    public static Agent moderatorAgent(LLMProvider llmProvider, String goal, List<Node<?>> agents, String model) {
        return DefaultModeratorAgent.of(llmProvider, model, goal, agents, CODING_CONTEXT_VARIABLE_TEMPLATE);
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
                .promptTemplate(Strings.format("""
                    {}
                    User's query:
                    """, CODING_CONTEXT_VARIABLE_TEMPLATE))
                .model(model)
                .useGroupContext(true)
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
                        If you are list file tree for java project, please consider these fixed paths to avoid wasting time searching through layers here as well:
                        - src/main/java - for java source code
                        - src/test/java - for java test code
                        - src/main/resources - for resources
                        If the non-recursive method of obtaining the directory tree exceeds 3 attempts, please consider using the recursive method.
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
