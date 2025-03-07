package com.chancetop.naixt.agent.service;

import ai.core.agent.AgentGroup;
import ai.core.agent.AgentRole;
import ai.core.llm.providers.AzureInferenceProvider;
import ai.core.persistence.providers.TemporaryPersistenceProvider;
import com.chancetop.naixt.agent.agent.CodingAgentGroup;
import com.chancetop.naixt.agent.api.naixt.ApproveChangeRequest;
import com.chancetop.naixt.agent.api.naixt.ChatResponse;
import com.chancetop.naixt.agent.api.naixt.NaixtChatRequest;
import com.chancetop.naixt.agent.utils.IdeUtils;
import core.framework.async.Executor;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.util.Strings;
import core.framework.web.sse.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
        this.codingAgentGroup = CodingAgentGroup.of(llmProvider, new TemporaryPersistenceProvider(), model, planningModel);
        this.isInitialized = true;
        // todo: init language server
        logger.info("initialized agent service with workspace: {}", workspacePath);
    }

    public void chatSse(NaixtChatRequest request, Channel<ChatResponse> channel) {
        request.model = Strings.strip(request.model);
        if (!isInitialized || !request.workspacePath.equals(workspacePath)) {
            init(request.workspacePath, request.model, request.planningModel);
        }
        codingAgentGroup.addMessageUpdatedEventListener((agent, message) -> {
            if (message.role == AgentRole.ASSISTANT && !message.name.equals("coding-agent")) {
                channel.send(ChatResponse.of(message.name + ": " + message.content));
            }
        });
        var rsp = chat(request);
        rsp.text = "coding-agent: " + rsp.text;
        channel.send(rsp);
    }

    public ChatResponse chat(NaixtChatRequest request) {
        request.model = Strings.strip(request.model);
        if (!isInitialized || !request.workspacePath.equals(workspacePath)) {
            init(request.workspacePath, request.model, request.planningModel);
        }
        var rsp = codingAgentGroup.run(request.query, buildContext(request));
        try {
            return toRsp(JSON.fromJSON(CodingAgentGroup.CodingResponse.class, rsp));
        } catch (Exception e) {
            logger.warn("Not coding result: {}", rsp);
            return ChatResponse.of(rsp);
        }
    }

    private Map<String, Object> buildContext(NaixtChatRequest request) {
        var context = new HashMap<String, Object>();
        context.put("workspace_path", request.workspacePath);
        context.put("current_file_path", IdeUtils.toWorkspaceRelativePath(request.workspacePath, request.currentFilePath));
        context.put("current_file_content", IdeUtils.getFileContent(request.currentFilePath));
        context.put("current_line_number", request.currentLineNumber);
        context.put("current_column_number", request.currentColumnNumber);
        return context;
    }

    private ChatResponse toRsp(CodingAgentGroup.CodingResponse codingResponse) {
        var rsp = ChatResponse.of(codingResponse.text);
        rsp.fileContents = codingResponse.fileContents;
        return rsp;
    }

    public void clear() {
        if (codingAgentGroup == null) return;
        codingAgentGroup.clearShortTermMemory();
    }

    public void approved(ApproveChangeRequest request) {
        executor.submit("do-change", () -> request.fileContents.forEach(fileContent -> IdeUtils.doChange(request.workspacePath, fileContent)));
    }
}
