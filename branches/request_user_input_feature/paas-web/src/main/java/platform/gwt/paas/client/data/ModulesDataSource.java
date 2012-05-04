package platform.gwt.paas.client.data;

import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.types.FieldType;
import paas.api.gwt.shared.dto.ModuleDTO;

public class ModulesDataSource extends DataSource {
    public ModulesDataSource(ModuleDTO[] dtos) {
        DataSourceField idField = new DataSourceField(ModuleRecord.ID_FIELD, FieldType.INTEGER);
        DataSourceField nameField = new DataSourceField(ModuleRecord.NAME_FIELD, FieldType.TEXT);
        setFields(idField, nameField);

        setClientOnly(true);

        setTestData(ModuleRecord.fromDTOs(dtos));
    }
}
