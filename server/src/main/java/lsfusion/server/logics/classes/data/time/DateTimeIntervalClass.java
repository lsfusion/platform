package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.time.LocalDateTime;

import static lsfusion.base.DateConverter.*;

public class DateTimeIntervalClass extends IntervalClass<LocalDateTime> {

    public final static IntervalClass instance = new DateTimeIntervalClass();

    private DateTimeIntervalClass() {
        super(LocalizedString.create("{classes.date.with.time.interval}"));
    }

    @Override
    public String getSID() {
        return "DATETIMEINTERVAL";
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DateTimeIntervalClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.DATETIMEINTERVAL;
    }

    @Override
    protected Long parse(String date) throws ParseException {
        return localDateTimeToUTCEpoch(DateTimeClass.instance.parseString(date));
    }

    @Override
    protected String format(Long epoch) {
        return DateTimeClass.instance.formatString(epochToLocalDateTime(epoch));
    }


    @Override
    protected String getSQLFrom(String source) {
        return "to_timestamp((trunc (" + source + "::NUMERIC)) / 1000) AT TIME ZONE 'UTC'";
    }

    @Override
    protected String getSQLTo(String source) {
        return "to_timestamp((SPLIT_PART(" + source + "::TEXT, '.', 2)::NUMERIC) / 1000) AT TIME ZONE 'UTC'";
    }
}
