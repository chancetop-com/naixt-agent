package com.chancetop.naixt.agent.service;

import ai.core.tool.function.annotation.CoreAiMethod;
import ai.core.tool.function.annotation.CoreAiParameter;
import com.chancetop.naixt.agent.utils.IdeUtils;

/**
 * @author stephen
 */
public class WorkspaceToolingService {
    private String workspacePath;

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    @CoreAiMethod(
            name = "get_file_content",
            description = "tool to get file content")
    public String getFileContent(
            @CoreAiParameter(
                    name = "file_path",
                    description = "the file path",
                    required = true) String filePath) {
        return IdeUtils.getFileContent(workspacePath, filePath);
    }

    @CoreAiMethod(
            name = "get_file_tree",
            description = "tool to get directory file tree")
    public String getDirFileTree(
            @CoreAiParameter(
                    name = "directory_path",
                    description = "the path of the directory",
                    required = true) String dir,
            @CoreAiParameter(
                    name = "recursive",
                    description = "whether to get the file tree recursively"
            ) Boolean recursive) {
        return IdeUtils.getDirFileTree(workspacePath, dir, recursive);
    }
}
