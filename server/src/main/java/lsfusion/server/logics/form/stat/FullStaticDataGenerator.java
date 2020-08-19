package lsfusion.server.logics.form.stat;

import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyReaderEntity;

// with headers, footers, etc.
public class FullStaticDataGenerator extends StaticDataGenerator<PropertyReaderEntity> {

    public FullStaticDataGenerator(FormDataInterface formInterface, Hierarchy hierarchy, boolean supportGroupColumns) {
        super(formInterface, hierarchy, supportGroupColumns);
    }

    @Override
    protected void fillQueryProps(PropertyDrawEntity property, MExclSet<PropertyReaderEntity> mResult) {
        mResult.exclAddAll(property.getQueryProps());
    }
}
