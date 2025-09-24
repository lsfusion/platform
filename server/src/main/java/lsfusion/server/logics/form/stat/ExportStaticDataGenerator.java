package lsfusion.server.logics.form.stat;

import lsfusion.server.logics.form.struct.property.PropertyReaderEntity;

public class ExportStaticDataGenerator extends StaticDataGenerator<PropertyReaderEntity> {

    public ExportStaticDataGenerator(FormDataInterface formInterface) {
        super(formInterface, false);
    }
}
