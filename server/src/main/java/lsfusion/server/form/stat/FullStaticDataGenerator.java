package lsfusion.server.form.stat;

import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.instance.FormDataInterface;

// with headers, footers, etc.
public class FullStaticDataGenerator extends StaticDataGenerator<PropertyReaderEntity> {

    public FullStaticDataGenerator(FormDataInterface formInterface, Hierarchy hierarchy, boolean supportGroupColumns) {
        super(formInterface, hierarchy, supportGroupColumns);
    }

    @Override
    protected void fillQueryProps(PropertyDrawEntity property, MExclSet<PropertyReaderEntity> mResult) {
        property.fillQueryProps(mResult);
    }
}
