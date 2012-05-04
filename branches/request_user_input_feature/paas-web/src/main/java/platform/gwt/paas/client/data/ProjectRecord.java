package platform.gwt.paas.client.data;

import paas.api.gwt.shared.dto.ProjectDTO;

public class ProjectRecord extends BasicRecord {
    public ProjectRecord() {
    }

    public ProjectRecord(int id, String name, String description) {
        super(id, name, description);
    }

    public static ProjectRecord fromDTO(ProjectDTO dto) {
        return new ProjectRecord(dto.id, dto.name, dto.description);
    }
}
