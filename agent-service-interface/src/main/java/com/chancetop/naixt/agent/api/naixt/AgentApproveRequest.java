package com.chancetop.naixt.agent.api.naixt;

import core.framework.api.json.Property;
import core.framework.api.validate.NotNull;

import java.util.List;

/**
 * @author stephen
 */
public class AgentApproveRequest {
    @Property(name = "workspace_path")
    public String workspacePath;

    @NotNull
    @Property(name = "file_contents")
    public List<FileContent> fileContents = List.of();
}
