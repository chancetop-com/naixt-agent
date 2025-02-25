package com.chancetop.naixt.agent.api.naixt;

import core.framework.api.json.Property;

/**
 * @author stephen
 */
public class FileContent {
    @Property(name = "content")
    public String content;

    @Property(name = "file_path")
    public String filePath;

    @Property(name = "action")
    public Action action;
}
