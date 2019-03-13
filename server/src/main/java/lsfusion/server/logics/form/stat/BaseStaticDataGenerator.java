package lsfusion.server.logics.form.stat;

import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;

public class BaseStaticDataGenerator extends StaticDataGenerator<PropertyDrawEntity> {

    public BaseStaticDataGenerator(FormDataInterface formInterface, Hierarchy hierarchy, boolean supportGroupColumns) {
        super(formInterface, hierarchy, supportGroupColumns);
    }
}
