package platform.gwt.paas.client.pages.project.config;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.ListGridField;
import paas.api.gwt.shared.dto.ConfigurationDTO;
import platform.gwt.paas.client.data.ConfigurationRecord;
import platform.gwt.paas.client.data.DTOConverter;
import platform.gwt.paas.client.widgets.BasicListGrid;

public class ConfigurationListGrid extends BasicListGrid {
    public ConfigurationListGrid() {
        setWidth100();
        setShowHeader(false);

        createFields();
    }

    private void createFields() {
        ListGridField nameField = new ListGridField(ConfigurationRecord.NAME_FIELD, "Name");
        nameField.setEscapeHTML(true);

        ListGridField statusField = new ListGridField("status", "Status");
        statusField.setType(ListGridFieldType.IMAGE);
        statusField.setAlign(Alignment.CENTER);
        statusField.setImageURLPrefix("icons/");
        statusField.setImageURLSuffix(".png");
        statusField.setValueIconSize(16);
        statusField.setWidth(50);

        setFields(nameField, statusField);
    }

    @Override
    protected DTOConverter createDTOConverter() {
        return new DTOConverter<ConfigurationDTO, ConfigurationRecord>() {
            @Override
            public ConfigurationRecord convert(ConfigurationDTO dto) {
                return ConfigurationRecord.fromDTO(dto);
            }
        };
    }
}
