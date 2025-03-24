package com.chancetop.naixt.agent.service;

import ai.core.agent.AgentGroup;
import ai.core.agent.AgentRole;
import ai.core.agent.planning.DefaultPlanning;
import ai.core.document.Document;
import ai.core.document.TextChunk;
import ai.core.document.textsplitters.RecursiveCharacterTextSplitter;
import ai.core.llm.providers.AzureInferenceProvider;
import ai.core.llm.providers.inner.EmbeddingRequest;
import ai.core.llm.providers.inner.Message;
import ai.core.persistence.providers.TemporaryPersistenceProvider;
import ai.core.rag.vectorstore.hnswlib.HnswConfig;
import ai.core.rag.vectorstore.hnswlib.HnswLibVectorStore;
import com.chancetop.naixt.agent.agent.CodingAgentGroup;
import com.chancetop.naixt.agent.agent.TaskSuggestionAgent;
import com.chancetop.naixt.agent.api.naixt.AgentApproveRequest;
import com.chancetop.naixt.agent.api.naixt.AgentChatResponse;
import com.chancetop.naixt.agent.api.naixt.AgentChatRequest;
import com.chancetop.naixt.agent.api.naixt.AgentSuggestionRequest;
import com.chancetop.naixt.agent.api.naixt.AgentSuggestionResponse;
import com.chancetop.naixt.agent.api.naixt.CurrentEditInfoView;
import com.chancetop.naixt.agent.utils.IdeUtils;
import core.framework.async.Executor;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.util.Strings;
import core.framework.web.sse.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
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

    public void init(String workspacePath, String model, String planningModel) {
        this.workspacePath = workspacePath;
        var vectorStorePath = Paths.get(workspacePath).resolve(".naixt/embeddings.bin");
        if (!vectorStorePath.toFile().exists()) {
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
                throw new RuntimeException("Init workspace failed: ", e);
            }
        }
        this.codingAgentGroup = CodingAgentGroup.of(llmProvider, new TemporaryPersistenceProvider(), model, planningModel, vectorStorePath.toString());
        this.isInitialized = true;
        // todo: init language server
        logger.info("initialized agent service with workspace: {}", workspacePath);
    }

    public void chatSse(AgentChatRequest request, Channel<AgentChatResponse> channel) {
        request.settings.model = Strings.strip(request.settings.model);
        if (!isInitialized || !request.editInfo.workspacePath.equals(workspacePath)) {
            init(request.editInfo.workspacePath, request.settings.model, request.settings.planningModel);
        }
        codingAgentGroup.addMessageUpdatedEventListener((agent, message) -> {
            if (message.role != AgentRole.ASSISTANT || message.name.equals("coding-agent")) return;
            if (message.name.equals(codingAgentGroup.getModerator().getName())) {
                var p = codingAgentGroup.getPlanning().localPlanning(message.content, DefaultPlanning.DefaultAgentPlanningResult.class);
                channel.send(AgentChatResponse.of(message.name + ": " + p.planning));
            } else {
                channel.send(AgentChatResponse.of(message.name + ": " + buildContent(message)));
            }
        });
        var rsp = chat(request);
        rsp.groupFinished = true;
        channel.send(rsp);
    }

    private String buildContent(Message message) {
        return message.content == null ? Strings.format("{}({})", message.toolCalls.getLast().function.name, message.toolCalls.getLast().function.arguments) : message.content;
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        request.settings.model = Strings.strip(request.settings.model);
        if (!isInitialized || !request.editInfo.workspacePath.equals(workspacePath)) {
            init(request.editInfo.workspacePath, request.settings.model, request.settings.planningModel);
        }
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

    private AgentChatResponse toRsp(CodingAgentGroup.CodingResponse codingResponse) {
        var rsp = AgentChatResponse.of(codingResponse.text);
        rsp.fileContents = codingResponse.fileContents;
        return rsp;
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

    private Map<String, Object> buildContext(CurrentEditInfoView editInfo) {
        var context = new HashMap<String, Object>();
        context.put("workspace_path", editInfo.workspacePath);
        context.put("current_file_path", IdeUtils.toWorkspaceRelativePath(editInfo.workspacePath, editInfo.currentFilePath));
        context.put("current_file_content", IdeUtils.getFileContent(editInfo.workspacePath, editInfo.currentFilePath));
        context.put("current_line_number", editInfo.currentLineNumber);
        context.put("current_column_number", editInfo.currentColumnNumber);
        context.put("current_file_diagnostic", editInfo.currentFileDiagnostic);
        return context;
    }
}
