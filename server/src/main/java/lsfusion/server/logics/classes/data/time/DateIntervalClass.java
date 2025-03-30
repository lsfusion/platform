package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.time.LocalDate;

import static lsfusion.base.DateConverter.*;

public class DateIntervalClass extends IntervalClass<LocalDate> {

    public final static IntervalClass instance = new DateIntervalClass();

    private DateIntervalClass() {
        super(LocalizedString.create("{classes.date.interval}"));
    }

    @Override
    public String getSID() {
        return "DATEINTERVAL";
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DateIntervalClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.DATEINTERVAL;
    }

    @Override
    protected Long parse(String date) throws ParseException {
        return localDateTimeToUTCEpoch(DateClass.instance.parseInterval(date).atStartOfDay());
    }

    @Override
    protected Long parseUIString(String date, String pattern) throws ParseException {
        return localDateTimeToUTCEpoch(DateClass.instance.parseIntervalUI(date, pattern).atStartOfDay());
    }

    @Override
    protected String format(Long epoch) {
        return DateClass.instance.formatInterval(epochToLocalDateTime(epoch).toLocalDate());
    }

    @Override
    protected String formatUI(Long epoch, String pattern) {
        return DateClass.instance.formatIntervalUI(epochToLocalDateTime(epoch).toLocalDate(), pattern);
    }

    @Override
    protected String getSQLFrom(String source) {
        return "(to_timestamp((trunc (" + source + "::NUMERIC)) / 1000) AT TIME ZONE 'UTC')::date";
    }

    @Override
    protected String getSQLTo(String source) {
        return "(to_timestamp((SPLIT_PART(" + source + "::TEXT, '.', 2)::NUMERIC) / 1000) AT TIME ZONE 'UTC')::date";
    }
}
