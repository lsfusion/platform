package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.data.type.DBType;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.TextBasedClass;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

public abstract class TimeSeriesClass<T extends Temporal> extends TextBasedClass<T> implements DBType {

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

    public T parseIntervalUI(String value) throws ParseException {
        return parseUI(value);
    }
    public T parseInterval(String value) throws ParseException {
        return parseString(value);
    }
    public String formatIntervalUI(T value) {
        return formatUI(value);
    }
    public String formatInterval(T value) {
        return formatString(value);
    }

    protected abstract T parseFormat(String value, DateTimeFormatter formatter) throws ParseException;

    @Override
    public T parseUI(String value) throws ParseException {
        return parseFormat(value, DateTimeFormatter.ofPattern(getDefaultPattern()));
    }

    @Override
    public String formatUI(T value) {
        if(value == null)
            return null;

        return DateTimeFormatter.ofPattern(getDefaultPattern()).format(value);
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.pattern = getDefaultPattern();
    }

    @Override
    public FlexAlignment getValueAlignmentHorz() {
        return FlexAlignment.END;
    }
}
