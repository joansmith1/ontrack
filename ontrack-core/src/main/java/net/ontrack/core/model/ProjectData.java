package net.ontrack.core.model;

import lombok.Data;
import org.codehaus.jackson.JsonNode;

/**
 * Export/import data for a project
 */
@Data
public class ProjectData {

    private final String name;
    private final JsonNode data;
}
