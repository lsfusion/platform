package lsfusion.server.form.stat;

import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.instance.FormDataInterface;

public class BaseStaticDataGenerator extends StaticDataGenerator<PropertyDrawEntity> {

    public BaseStaticDataGenerator(FormDataInterface formInterface, Hierarchy hierarchy, boolean supportGroupColumns) {
        super(formInterface, hierarchy, supportGroupColumns);
    }
}
