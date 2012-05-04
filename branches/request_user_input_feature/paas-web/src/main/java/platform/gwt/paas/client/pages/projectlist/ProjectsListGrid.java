package platform.gwt.paas.client.pages.projectlist;

import com.smartgwt.client.widgets.grid.ListGridField;
import paas.api.gwt.shared.dto.ProjectDTO;
import platform.gwt.paas.client.data.DTOConverter;
import platform.gwt.paas.client.data.ProjectRecord;
import platform.gwt.paas.client.widgets.BasicListGrid;

public class ProjectsListGrid extends BasicListGrid {

    public ProjectsListGrid() {
        setWidth100();
        createFields();
    }

    @Override
    protected DTOConverter createDTOConverter() {
        return new DTOConverter<ProjectDTO, ProjectRecord>() {
            @Override
            public ProjectRecord convert(ProjectDTO dto) {
                return ProjectRecord.fromDTO(dto);
            }
        };
    }

    private void createFields() {
        ListGridField nameField = new ListGridField(ProjectRecord.NAME_FIELD, "Projects");
        nameField.setEscapeHTML(true);

        setFields(nameField);
    }
}
