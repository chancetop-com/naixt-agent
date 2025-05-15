package com.chancetop.naixt.agent.service;

import ai.core.agent.AgentGroup;
import ai.core.agent.AgentRole;
import ai.core.agent.Node;
import ai.core.agent.handoff.handoffs.HybridAutoDirectHandoff;
import ai.core.agent.planning.plannings.DefaultPlanningResult;
import ai.core.document.Document;
import ai.core.document.TextChunk;
import ai.core.document.textsplitters.RecursiveCharacterTextSplitter;
import ai.core.llm.providers.AzureInferenceProvider;
import ai.core.llm.providers.inner.EmbeddingRequest;
import ai.core.llm.providers.inner.LLMMessage;
import ai.core.mcp.client.MCPServerConfig;
import ai.core.persistence.providers.TemporaryPersistenceProvider;
import ai.core.rag.vectorstore.hnswlib.HnswConfig;
import ai.core.rag.vectorstore.hnswlib.HnswLibVectorStore;
import com.chancetop.naixt.agent.agent.CodingAgentGroup;
import com.chancetop.naixt.agent.agent.NaixtAgentGroup;
import com.chancetop.naixt.agent.agent.TaskSuggestionAgent;
import com.chancetop.naixt.agent.api.naixt.AgentApproveRequest;
import com.chancetop.naixt.agent.api.naixt.AgentChatResponse;
import com.chancetop.naixt.agent.api.naixt.AgentChatRequest;
import com.chancetop.naixt.agent.api.naixt.AgentSuggestionRequest;
import com.chancetop.naixt.agent.api.naixt.AgentSuggestionResponse;
import com.chancetop.naixt.agent.api.naixt.CurrentEditInfoView;
import com.chancetop.naixt.agent.api.naixt.NaixtPluginMcpSettingView;
import com.chancetop.naixt.agent.api.naixt.NaixtPluginSettingsView;
import com.chancetop.naixt.agent.utils.IdeUtils;
import core.framework.async.Executor;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.util.Strings;
import core.framework.web.sse.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author stephen
 */
public class NaixtAgentService {
    private final Logger logger = LoggerFactory.getLogger(NaixtAgentService.class);
    @Inject
    AzureInferenceProvider llmProvider;
    @Inject
    Executor executor;

    private boolean isInitialized = false;
    private AgentGroup codingAgentGroup;
    private String workspacePath;
    private NaixtPluginSettingsView settings;

    public void init(AgentChatRequest request, Channel<AgentChatResponse> channel) {
        this.settings = request.settings;
        this.workspacePath = request.editInfo.workspacePath;
        var vectorStorePath = Paths.get(workspacePath).resolve(".naixt/embeddings.bin");
        if (!vectorStorePath.toFile().exists()) {
            initVectorStore(vectorStorePath);
        }
        if (request.settings.atlassianEnabled != null && request.settings.atlassianEnabled) {
            var mcpServerConfigs = setupMcpServerConfigs(request);
            this.codingAgentGroup = NaixtAgentGroup.of(new NaixtAgentGroup.NaixtAgentGroupConfig(
                    llmProvider,
                    new TemporaryPersistenceProvider(),
                    request.settings.model,
                    request.settings.planningModel,
                    vectorStorePath.toString(),
                    this.workspacePath,
                    mcpServerConfigs));
        } else {
            this.codingAgentGroup = CodingAgentGroup.of(llmProvider, new TemporaryPersistenceProvider(), request.settings.model, request.settings.planningModel, vectorStorePath.toString(), workspacePath);
        }
        this.isInitialized = true;
        // todo: init language server if in cloud env
        if (channel != null) {
            sendInitMessage(request, channel);
        }
    }

    public void chatSse(AgentChatRequest request, Channel<AgentChatResponse> channel) {
        var rsp = chat(request, channel);
        rsp.groupFinished = true;
        channel.send(rsp);
    }

    public AgentChatResponse chat(AgentChatRequest request, Channel<AgentChatResponse> channel) {
        request.settings.model = Strings.strip(request.settings.model);
        if (!isInitialized || !request.editInfo.workspacePath.equals(workspacePath) || settingChanged(request.settings)) {
            init(request, channel);
        }
        if (channel != null) setMessageUpdatedEventListener(channel);
        var rsp = codingAgentGroup.run(request.query, buildContext(request.editInfo));
        try {
            var response = toRsp(JSON.fromJSON(CodingAgentGroup.CodingResponse.class, rsp));
            response.text = "coding-agent:\n" + response.text;
            return response;
        } catch (Exception e) {
            logger.warn("Not coding result: {}", rsp);
            return AgentChatResponse.of(rsp);
        }
    }

    public void clear() {
        if (codingAgentGroup == null) return;
        codingAgentGroup.clearShortTermMemory();
    }

    public void approved(AgentApproveRequest request) {
        executor.submit("do-change", () -> request.fileContents.forEach(fileContent -> IdeUtils.doChange(request.workspacePath, fileContent)));
    }

    public AgentSuggestionResponse suggestion(AgentSuggestionRequest request) {
        return AgentSuggestionResponse.of(List.of(TaskSuggestionAgent.of(llmProvider, request.settings.model).run("", buildContext(request.editInfo)).split("\n")));
    }

    private boolean settingChanged(NaixtPluginSettingsView settings) {
        return !this.settings.model.equalsIgnoreCase(settings.model)
                || !this.settings.planningModel.equalsIgnoreCase(settings.planningModel)
                || this.settings.atlassianEnabled != settings.atlassianEnabled
                || mcpConfigChanged(this.settings.atlassianMcpSetting);
    }

    private boolean mcpConfigChanged(NaixtPluginMcpSettingView jiraMcpSetting) {
        return !this.settings.atlassianMcpSetting.url.equalsIgnoreCase(jiraMcpSetting.url);
    }

    private String buildContent(LLMMessage message) {
        return Strings.isBlank(message.content) ? Strings.format("{}({})", message.toolCalls.getLast().function.name, message.toolCalls.getLast().function.arguments) : message.content;
    }

    private void initVectorStore(Path vectorStorePath) {
        try {
            Files.createDirectories(vectorStorePath.getParent());
            Files.createFile(vectorStorePath);
            var fileTree = IdeUtils.getDirFileTree(workspacePath, workspacePath, true);
            var chunks = new RecursiveCharacterTextSplitter().split(fileTree);
            var embeddingTexts = chunks.stream().map(TextChunk::embeddingText).toList();
            var rsp = llmProvider.embeddings(new EmbeddingRequest(embeddingTexts));
            var documents = rsp.embeddings.stream().map(v -> new Document(v.text, v.embedding, null)).toList();
            HnswLibVectorStore.build(HnswConfig.of(vectorStorePath.toString()), documents);
        } catch (Exception e) {
            logger.warn("Init workspace failed: ", e);
        }
    }

    private void setMessageUpdatedEventListener(Channel<AgentChatResponse> channel) {
        codingAgentGroup.setMessageUpdatedEventListener((agent, message) -> messageHandler(channel, agent, message));
    }

    private void sendInitMessage(AgentChatRequest request, Channel<AgentChatResponse> channel) {
        var rsp = AgentChatResponse.of(Strings.format("Initialized\nAgent service with workspace: {}\nModel: {}\nPlanning Model: {}\n", workspacePath, request.settings.model, request.settings.planningModel));
        if (request.settings.atlassianEnabled) {
            rsp.text += Strings.format("Atlassian enabled: {}\n", request.settings.atlassianMcpSetting.url);
        }
        channel.send(rsp);
    }

    public void messageHandler(Channel<AgentChatResponse> channel, Node<?> node, LLMMessage message) {
        if (message.role != AgentRole.ASSISTANT || message.name.equals("coding-agent")) return;
        if (message.name.equals(((HybridAutoDirectHandoff) codingAgentGroup.getHandoff()).getAutoHandoff().moderator().getName())) {
            var p = codingAgentGroup.getPlanning().explainPlanning(message.content, DefaultPlanningResult.class);
            channel.send(AgentChatResponse.of(Strings.format("{}[{}]: {}", message.name, getAgentGroupName(message, node.getName()), p.planning)));
        } else {
            channel.send(AgentChatResponse.of(Strings.format("{}[{}]: {}", message.name, getAgentGroupName(message, node.getName()), buildContent(message))));
        }
    }

    private String getAgentGroupName(LLMMessage message, String name) {
        return message.groupName != null ? message.groupName : message.agentName != null ? message.agentName : name;
    }

    private List<MCPServerConfig> setupMcpServerConfigs(AgentChatRequest request) {
        var mcpServerConfigs = new ArrayList<MCPServerConfig>();
        if (request.settings.atlassianEnabled != null && request.settings.atlassianEnabled) {
            if (request.settings.atlassianMcpSetting == null || request.settings.atlassianMcpSetting.url == null) throw new RuntimeException("atlassian mcp setting is required when jira is enabled.");
            mcpServerConfigs.add(new MCPServerConfig(request.settings.atlassianMcpSetting.url, "atlassian-agent", "fetch information from atlassian products like jira and wiki"));
        }
        return mcpServerConfigs.isEmpty() ? null : mcpServerConfigs;
    }

    private AgentChatResponse toRsp(CodingAgentGroup.CodingResponse codingResponse) {
        var rsp = AgentChatResponse.of(codingResponse.text);
        rsp.fileContents = codingResponse.fileContents;
        return rsp;
    }

    private Map<String, Object> buildContext(CurrentEditInfoView editInfo) {
        var context = new HashMap<String, Object>();
        context.put("workspace_path", editInfo.workspacePath);
        context.put("current_file_path", IdeUtils.toWorkspaceRelativePath(editInfo.workspacePath, editInfo.currentFilePath));
        context.put("current_file_content", IdeUtils.getFileContent(editInfo.workspacePath, editInfo.currentFilePath));
        context.put("current_line_number", editInfo.currentLineNumber);
        context.put("current_column_number", editInfo.currentColumnNumber);
        context.put("current_file_diagnostic", editInfo.currentFileDiagnostic);
        context.put("system_environment", IdeUtils.getSystemVersion());
        return context;
    }
}
