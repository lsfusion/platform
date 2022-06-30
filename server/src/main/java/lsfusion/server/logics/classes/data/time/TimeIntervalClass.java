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
        return localDateTimeToUTCEpoch(TimeClass.instance.parseString(date).atDate(dateEpoch));
    }

    @Override
    protected String format(Long epoch) {
        return TimeClass.instance.formatString(epochToLocalDateTime(epoch).toLocalTime());
    }
}
