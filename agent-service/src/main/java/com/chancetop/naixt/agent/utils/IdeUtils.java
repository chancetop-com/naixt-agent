package com.chancetop.naixt.agent.utils;

import com.chancetop.naixt.agent.api.naixt.Action;
import com.chancetop.naixt.agent.api.naixt.FileContent;
import core.framework.util.Strings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author stephen
 */
public class IdeUtils {
    private static final Logger LOGGER = Logger.getLogger(IdeUtils.class.getName());

    public static String getFileContent(String workspacePath, String path) {
        if (Strings.isBlank(path)) return "Path is blank.";
        var truePath = toAbsolutePath(workspacePath, path);
        try {
            return Files.readString(Paths.get(truePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warning("Failed to read file: " + path);
            return Strings.format("failed to read file<{}>, please check your path", path);
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

    public static String getDirFileTree(String workspacePath, String path, Boolean recursive) {
        if (Strings.isBlank(path)) return "";
        try {
            var truePath = toAbsolutePath(workspacePath, path);
            var rootPath = Paths.get(truePath);
            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                return "Invalid workspace directory.";
            }
            return buildDirFileTree(rootPath, recursive, new StringBuilder(), true) + "\n";
        } catch (IOException e) {
            LOGGER.warning("Failed to read workspace directory: " + path);
            return "Error reading workspace directory, please check your path.";
        }
    }

    private static String buildDirFileTree(Path dir, boolean recursive, StringBuilder indent, boolean isLast) throws IOException {
        var sb = new StringBuilder();
        var dirName = dir.getFileName().toString();
        sb.append(indent);

        StringBuilder newIndent;
        if (isLast) {
            sb.append("└── ");
            newIndent = new StringBuilder(indent).append("    ");
        } else {
            sb.append("├── ");
            newIndent = new StringBuilder(indent).append("│   ");
        }
        sb.append(dirName).append("/\n");

        var entries = new ArrayList<Path>();
        try (var stream = Files.newDirectoryStream(dir)) {
            for (var entry : stream) {
                entries.add(entry);
            }
        }

        entries.sort((p1, p2) -> {
            var isDir1 = Files.isDirectory(p1);
            var isDir2 = Files.isDirectory(p2);
            if (isDir1 != isDir2) {
                return isDir1 ? -1 : 1;
            }
            return p1.getFileName().toString().compareToIgnoreCase(p2.getFileName().toString());
        });

        for (var i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var entryIsLast = i == entries.size() - 1;

            if (isGitIgnore(entry)) continue;

            if (Files.isDirectory(entry)) {
                if (recursive) {
                    var subtree = buildDirFileTree(entry, true, newIndent, entryIsLast);
                    sb.append(subtree);
                } else {
                    sb.append(newIndent).append(entryIsLast ? "└── " : "├── ").append(entry.getFileName().toString()).append("/\n");
                }
            } else {
                sb.append(newIndent).append(entryIsLast ? "└── " : "├── ").append(entry.getFileName().toString()).append("\n");
            }
        }

        return sb.toString();
    }

    private static boolean isGitIgnore(Path path) {
        var entry = path.getFileName();
        return entry.toString().startsWith(".")
                || "build".equals(entry.toString())
                || "gradle".equals(entry.toString())
                || "bin".equals(entry.toString());
    }

    private static boolean filterFile(Path entry) {
        return entry.toString().endsWith(".java")
                || entry.toString().endsWith(".kt")
                || entry.toString().endsWith(".xml")
                || entry.toString().endsWith(".properties");
    }
}
