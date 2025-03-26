package com.chancetop.naixt.agent.agent;

import ai.core.agent.Agent;
import ai.core.agent.AgentGroup;
import ai.core.agent.Node;
import ai.core.agent.handoff.HandoffType;
import ai.core.defaultagents.DefaultModeratorAgent;
import ai.core.llm.LLMProvider;
import ai.core.mcp.client.MCPClientService;
import ai.core.mcp.client.MCPServerConfig;
import ai.core.persistence.PersistenceProvider;
import ai.core.tool.mcp.MCPToolCalls;
import core.framework.util.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * @author stephen
 */
public class NaixtAgentGroup {

    public static Agent moderatorAgent(LLMProvider llmProvider, String goal, List<Node<?>> agents, String model) {
        return DefaultModeratorAgent.of(llmProvider, model, goal, agents, CodingAgentGroup.CODING_CONTEXT_VARIABLE_TEMPLATE);
    }

    public static Agent mcpAgent(LLMProvider llmProvider, MCPServerConfig config) {
        var mcpService = new MCPClientService(config);
        return Agent.builder()
                .name(config.name())
                .description(config.description())
                .systemPrompt(Strings.format("""
                        You are assistant that helps users to use tools of {}.
                        """, config.name()))
                .promptTemplate("""
                        query:
                        """)
                .toolCalls(MCPToolCalls.from(mcpService))
                .llmProvider(llmProvider).build();
    }

    public static AgentGroup of(NaixtAgentGroupConfig config) {
        var requirementAgent = Agent.builder()
                .name("requirement-agent")
                .description("requirement-agent is an agent that provide user's requirement and analysis result.")
                .systemPrompt("""
                        You are an assistant that helps users analysis requirement.
                        You need to analyze the user's requirements and provide guidance to the coding-agent so that the coding-agent can clearly understand what coding tasks need to be performed.
                        """)
                .promptTemplate("""
                        query:
                        """)
                .llmProvider(config.llmProvider()).build();

        var codingAgentGroup = CodingAgentGroup.of(config.llmProvider(), config.persistenceProvider(), config.model(), config.planningModel(), config.vectorStorePath());

        var goal = """
        naixt-agent-group is a group of agents that help user to analysis requirement by fetch information from atlassian products and write code for it.
        If we got the requirement from atlassian, please provide the requirement analysis detail result of requirement-agent to coding-agent-group for coding.
        If the coding-agent finished coding, task completed.
        """;

        List<Node<?>> agents = new ArrayList<>(List.of(requirementAgent, codingAgentGroup));
        if (!config.mcpServerConfigs.isEmpty()) {
            var mcpAgents = config.mcpServerConfigs.stream().map(c -> mcpAgent(config.llmProvider(), c)).toList();
            agents.addAll(mcpAgents);
        }
        var moderatorAgent = moderatorAgent(config.llmProvider(), goal, agents, config.model());

        agents.stream().filter(v -> v.getName().contains("atlassian")).findFirst().ifPresent(v -> v.setNext(requirementAgent));
        requirementAgent.setNext(codingAgentGroup);

        return AgentGroup.builder()
                .agents(agents)
                .name("naixt-agent-group")
                .description(goal)
                .persistenceProvider(config.persistenceProvider())
                .moderator(moderatorAgent)
                .maxRound(3)
                .handoffType(HandoffType.HYBRID)
                .llmProvider(config.llmProvider()).build();
    }

    public record NaixtAgentGroupConfig(LLMProvider llmProvider,
                                        PersistenceProvider persistenceProvider,
                                        String model,
                                        String planningModel,
                                        String vectorStorePath,
                                        List<MCPServerConfig> mcpServerConfigs) {

    }
}
