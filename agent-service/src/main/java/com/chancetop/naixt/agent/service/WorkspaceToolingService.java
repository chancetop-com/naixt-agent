package com.chancetop.naixt.agent.service;

import ai.core.api.tool.function.CoreAiMethod;
import ai.core.api.tool.function.CoreAiParameter;
import com.chancetop.naixt.agent.utils.IdeUtils;

/**
 * @author stephen
 */
public class WorkspaceToolingService {
    private final String workspacePath;

    public WorkspaceToolingService(String workspacePath) {
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
                    description = "whether to get the file tree recursively, default is false, if we encounter a problem with the context length being too long, try setting this parameter to false. "
            ) Boolean recursive) {
        return IdeUtils.getDirFileTree(workspacePath, dir, recursive);
    }

    @CoreAiMethod(
            name = "search_file_by_name",
            description = "tool to search file by name, search in the directory recursively and support regex")
    public String searchFileByName(
            @CoreAiParameter(
                    name = "pattern",
                    description = "the file name, support regex",
                    required = true) String pattern,
            @CoreAiParameter(
                    name = "regex",
                    description = "is regex, default is false"
            ) Boolean regex,
            @CoreAiParameter(
                    name = "recursive",
                    description = "whether to get the file tree recursively, default is true."
            ) Boolean recursive) {
        return String.join(",", IdeUtils.searchFileByName(workspacePath, pattern, regex, recursive));
    }
}
