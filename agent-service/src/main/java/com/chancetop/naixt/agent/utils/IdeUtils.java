package com.chancetop.naixt.agent.utils;

import com.chancetop.naixt.agent.api.naixt.Action;
import com.chancetop.naixt.agent.api.naixt.FileContent;
import core.framework.util.Strings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
            return e.getMessage();
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

    public static void doChange(String workspace, FileContent fileContent) {
        if (fileContent == null || Strings.isBlank(fileContent.filePath)) return;
        if (fileContent.action == Action.DELETE) {
            try {
                Files.deleteIfExists(Paths.get(toAbsolutePath(workspace, fileContent.filePath)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        try {
            var path = Paths.get(toAbsolutePath(workspace, fileContent.filePath));
            if (fileContent.action == Action.ADD) {
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
            }
            Files.writeString(path, fileContent.content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toAbsolutePath(String workspace, String filePath) {
        Path absolutePath;
        var path = Paths.get(filePath);
        if (path.isAbsolute()) {
            absolutePath = path;
        } else {
            var workspacePath = Paths.get(workspace);
            absolutePath = workspacePath.resolve(filePath).normalize();
        }
        return absolutePath.toString();
    }

    public static String getDirFileTree(String path, Boolean recursive) {
        if (Strings.isBlank(path)) return "";

        var rootPath = Paths.get(path);
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            return "Invalid workspace directory.";
        }

        try {
            return buildDirFileTree(rootPath, recursive) + "\n";
        } catch (IOException e) {
            LOGGER.warning("Failed to read workspace directory: " + path);
            return "Error reading workspace directory.";
        }
    }

    private static String buildDirFileTree(Path current, Boolean recursive) throws IOException {
        var treeBuilder = new StringBuilder();

        try (var stream = Files.newDirectoryStream(current, entry -> Files.isRegularFile(entry) && filterFile(entry) || Files.isDirectory(entry))) {
            for (var entry : stream) {
                if (isGitIgnore(entry)) continue;
                if (Files.isRegularFile(entry)) {
                    treeBuilder.append(entry).append('\n');
                }
                if (recursive && Files.isDirectory(entry)) {
                    var subTree = buildDirFileTree(entry, true);
                    if (subTree.isEmpty()) continue;
                    treeBuilder.append(subTree).append('\n');
                }
            }
        }

        return treeBuilder.isEmpty() ? "" : treeBuilder.toString().replaceAll("\n\\s*\\n", "\n");
    }

    private static boolean isGitIgnore(Path path) {
        var entry = path.getFileName();
        return entry.toString().startsWith(".")
                || "build".equals(entry.toString())
                || "gradle".equals(entry.toString())
                || "bin".equals(entry.toString());
    }

    private static boolean filterFile(Path entry) {
        return entry.toString().endsWith(".java") || entry.toString().endsWith(".kt") || entry.toString().endsWith(".properties");
    }
}
