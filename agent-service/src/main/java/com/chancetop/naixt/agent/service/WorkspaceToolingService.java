package com.chancetop.naixt.agent.service;

import ai.core.tool.function.annotation.CoreAiMethod;
import ai.core.tool.function.annotation.CoreAiParameter;
import com.chancetop.naixt.agent.utils.IdeUtils;

/**
 * @author stephen
 */
public class WorkspaceToolingService {
    @CoreAiMethod(
            name = "get_file_contents",
            description = "tool to get file content")
    public String getFileContent(
            @CoreAiParameter(
                    name = "file_path",
                    description = "the file path",
                    required = true) String filePath) {
        return IdeUtils.getFileContent(filePath);
    }

    @CoreAiMethod(
            name = "get_workspace_file_tree",
            description = "tool to get workspace file tree")
    public String getWorkspaceFileTree(
            @CoreAiParameter(
                    name = "workspace_path",
                    description = "the workspace path",
                    required = true) String workspacePath) {
        return IdeUtils.getWorkspaceFileTree(workspacePath);
    }
}
