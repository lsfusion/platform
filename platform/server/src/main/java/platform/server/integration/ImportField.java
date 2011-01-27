package platform.server.integration;

import platform.server.classes.DataClass;

/**
 * User: DAle
 * Date: 06.12.2010
 * Time: 14:27:14
 */

public class ImportField {
    private DataClass fieldClass;

    public ImportField(DataClass fieldClass) {
        this.fieldClass = fieldClass;
    }

    public DataClass getFieldClass() {
        return fieldClass;
    }
}
