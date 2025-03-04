package com.chancetop.naixt.agent.service;

import ai.core.agent.AgentGroup;
import ai.core.llm.providers.AzureOpenAIProvider;
import ai.core.persistence.providers.TemporaryPersistenceProvider;
import com.chancetop.naixt.agent.agent.CodingAgentGroup;
import com.chancetop.naixt.agent.api.naixt.ApproveChangeRequest;
import com.chancetop.naixt.agent.api.naixt.ChatResponse;
import com.chancetop.naixt.agent.api.naixt.NaixtChatRequest;
import com.chancetop.naixt.agent.utils.IdeUtils;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @author stephen
 */
public class NaixtAgentService {
    private final Logger logger = LoggerFactory.getLogger(NaixtAgentService.class);
    @Inject
    AzureOpenAIProvider llmProvider;

    private boolean isInitialized = false;
//    private Agent agent;
    private AgentGroup codingAgentGroup;
    private String workspacePath;

    public void init(String workspacePath, String model) {
        this.workspacePath = workspacePath;
        this.codingAgentGroup = CodingAgentGroup.of(llmProvider, new TemporaryPersistenceProvider(), model);
//        this.agent = CodingAgentGroup.codingAgent(llmProvider, model);
        this.isInitialized = true;
        // todo: init language server
        logger.info("initialized agent service with workspace: {}", workspacePath);
    }

    public ChatResponse chat(NaixtChatRequest request) {
        request.model = Strings.strip(request.model);
        if (!isInitialized || !request.workspacePath.equals(workspacePath)) {
            init(request.workspacePath, request.model);
        }

        var context = new HashMap<String, Object>();
        context.put("workspace_path", request.workspacePath);
        context.put("current_file_path", IdeUtils.toWorkspaceRelativePath(request.workspacePath, request.currentFilePath));
        context.put("current_file_content", IdeUtils.getFileContent(request.currentFilePath));
        context.put("current_line_number", request.currentLineNumber);
        context.put("current_column_number", request.currentColumnNumber);
        var rsp = codingAgentGroup.run(request.query, context);
//        var rsp = agent.run(request.query, context);
        try {
            return JSON.fromJSON(ChatResponse.class, rsp);
        } catch (Exception e) {
            logger.warn("Not coding result: {}", rsp);
            return ChatResponse.of(rsp);
        }
    }

    public void clear() {
        if (codingAgentGroup == null) return;
        codingAgentGroup.clearShortTermMemory();
    }

    public void approved(ApproveChangeRequest request) {
        request.fileContents.forEach(fileContent -> IdeUtils.doChange(request.workspacePath, fileContent));
    }
}
