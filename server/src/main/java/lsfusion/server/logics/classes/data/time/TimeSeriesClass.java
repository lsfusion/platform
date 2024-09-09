package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.data.type.DBType;
import lsfusion.server.logics.classes.data.TextBasedClass;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class TimeSeriesClass<T> extends TextBasedClass<T> implements DBType {

    public TimeSeriesClass(LocalizedString caption) {
        super(caption);
    }

    @Override
    public DBType getDBType() {
        return this;
    }

    public abstract String getIntervalProperty();
    public abstract String getFromIntervalProperty();
    public abstract String getToIntervalProperty();

    public abstract String getDefaultPattern();

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.pattern = getDefaultPattern();
    }

    @Override
    public FlexAlignment getValueAlignmentHorz() {
        return FlexAlignment.END;
    }
}
