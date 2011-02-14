package platform.server.integration;

import platform.server.classes.DataClass;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;

/**
 * User: DAle
 * Date: 06.12.2010
 * Time: 14:27:14
 */

public class ImportField implements ImportFieldInterface {
    private DataClass fieldClass;

    public ImportField(DataClass fieldClass) {
        this.fieldClass = fieldClass;
    }

    public ImportField(LP<?> property) {
        this(property.property);
    }

    public ImportField(Property<?> property) {
        this.fieldClass = (DataClass) property.getCommonClasses().value;
    }

    public DataClass getFieldClass() {
        return fieldClass;
    }

    public DataObject getDataObject(ImportTable.Row row) {
        if (row.getValue(this) != null) {
            return new DataObject(row.getValue(this), getFieldClass());
        } else {
            return null;
        }
    }
}
