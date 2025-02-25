package com.chancetop.naixt.agent.utils;

import core.framework.util.Strings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * @author stephen
 */
public class IdeUtils {
    private static final Logger LOGGER = Logger.getLogger(IdeUtils.class.getName());

    public static String getFileContent(String path) {
        if (Strings.isBlank(path)) return "";
        try {
            return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warning("Failed to read file: " + path);
            return "";
        }
    }

    public static String toWorkspaceRelativePath(String workspacePath, String currentFilePath) {
        if (Strings.isBlank(workspacePath) || Strings.isBlank(currentFilePath)) return currentFilePath;

        var absolutePath = Paths.get(currentFilePath);
        var basePath = Paths.get(workspacePath);

        try {
            absolutePath = absolutePath.toRealPath();
            basePath = basePath.toRealPath();
        } catch (IOException e) {
            LOGGER.warning("Failed to resolve real path.");
            return currentFilePath;
        }

        if (!absolutePath.startsWith(basePath)) {
            return currentFilePath;
        }

        return basePath.relativize(absolutePath).toString();
    }
}
