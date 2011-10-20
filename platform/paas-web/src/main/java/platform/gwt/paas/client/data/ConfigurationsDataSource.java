package platform.gwt.paas.client.data;

import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.types.FieldType;
import paas.api.gwt.shared.dto.ConfigurationDTO;

public class ConfigurationsDataSource extends DataSource {

    private ConfigurationRecord[] data;

    public ConfigurationsDataSource(ConfigurationDTO[] dtos) {
        DataSourceField idField = new DataSourceField(ConfigurationRecord.ID_FIELD, FieldType.INTEGER);
        DataSourceField nameField = new DataSourceField(ConfigurationRecord.NAME_FIELD, FieldType.TEXT);
        setFields(idField, nameField);

        setClientOnly(true);

        data = ConfigurationRecord.fromDTOs(dtos);
        setTestData(data);
    }

    public ConfigurationRecord getRecord(int configurationId) {
        for (ConfigurationRecord record : data) {
            if (record.getId() == configurationId) {
                return record;
            }
        }
        return null;
    }
}
