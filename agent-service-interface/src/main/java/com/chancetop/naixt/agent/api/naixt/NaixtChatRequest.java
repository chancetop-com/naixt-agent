package com.chancetop.naixt.agent.api.naixt;

import com.google.gson.annotations.SerializedName;
import core.framework.api.json.Property;

/**
 * @author stephen
 */
public class NaixtChatRequest {
    @SerializedName("query")
    @Property(name = "query")
    public String query;

    @SerializedName("current_file_path")
    @Property(name = "current_file_path")
    public String currentFilePath;

    @SerializedName("current_line_number")
    @Property(name = "current_line_number")
    public Integer currentLineNumber;

    @SerializedName("current_column_number")
    @Property(name = "current_column_number")
    public Integer currentColumnNumber;

    @SerializedName("model")
    @Property(name = "model")
    public String model;

    @SerializedName("workspace_path")
    @Property(name = "workspace_path")
    public String workspacePath;
}
