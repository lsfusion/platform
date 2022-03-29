package lsfusion.server.logics.form.stat;

import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.server.logics.form.stat.print.FormReportInterface;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyReaderEntity;

// with headers, footers, etc.
public class ReportStaticDataGenerator extends StaticDataGenerator<PropertyReaderEntity> {

    public ReportStaticDataGenerator(FormReportInterface formInterface) {
        super(formInterface, true);
    }

    @Override
    protected void fillQueryProps(PropertyDrawEntity property, MExclSet<PropertyReaderEntity> mResult) {
        mResult.exclAddAll(property.getQueryProps());
    }
}
