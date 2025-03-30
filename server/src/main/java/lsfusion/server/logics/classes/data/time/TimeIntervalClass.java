package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.time.LocalDate;
import java.time.LocalTime;

import static lsfusion.base.DateConverter.epochToLocalDateTime;
import static lsfusion.base.DateConverter.localDateTimeToUTCEpoch;

public class TimeIntervalClass extends IntervalClass<LocalTime> {

    public final static IntervalClass instance = new TimeIntervalClass();

    private TimeIntervalClass() {
        super(LocalizedString.create("{classes.time.interval}"));
    }

    @Override
    public String getSID() {
        return "TIMEINTERVAL";
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof TimeIntervalClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.TIMEINTERVAL;
    }

    // should correspond ClientTimeIntervalClass, GTimeDTO.toTime
    private static final LocalDate dateEpoch = LocalDate.of(1970, 1, 1); //date before 1970 year gives a negative number of milliseconds
    @Override
    protected Long parse(String date) throws ParseException {
        return localDateTimeToUTCEpoch(TimeClass.instance.parseInterval(date).atDate(dateEpoch));
    }

    @Override
    protected Long parseUIString(String date, String pattern) throws ParseException {
        return localDateTimeToUTCEpoch(TimeClass.instance.parseIntervalUI(date, pattern).atDate(dateEpoch));
    }

    @Override
    protected String format(Long epoch) {
        return TimeClass.instance.formatInterval(epochToLocalDateTime(epoch).toLocalTime());
    }

    @Override
    protected String formatUI(Long epoch, String pattern) {
        return TimeClass.instance.formatIntervalUI(epochToLocalDateTime(epoch).toLocalTime(), pattern);
    }

    @Override
    protected String getSQLFrom(String source) {
        return "(to_timestamp((trunc (" + source + "::NUMERIC) / 1000)) AT TIME ZONE 'UTC')::time";
    }

    @Override
    protected String getSQLTo(String source) {
        return "(to_timestamp((SPLIT_PART(" + source + "::TEXT, '.', 2)::NUMERIC) / 1000) AT TIME ZONE 'UTC')::time";
    }
}
