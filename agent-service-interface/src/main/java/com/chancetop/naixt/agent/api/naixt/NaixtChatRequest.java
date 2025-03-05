package com.chancetop.naixt.agent.api.naixt;

import core.framework.api.json.Property;

/**
 * @author stephen
 */
public class NaixtChatRequest {
    @Property(name = "query")
    public String query;

    @Property(name = "current_file_path")
    public String currentFilePath;

    @Property(name = "current_line_number")
    public Integer currentLineNumber;

    @Property(name = "current_column_number")
    public Integer currentColumnNumber;

    @Property(name = "model")
    public String model;

    @Property(name = "planning_model")
    public String planningModel;

    @Property(name = "workspace_path")
    public String workspacePath;
}
