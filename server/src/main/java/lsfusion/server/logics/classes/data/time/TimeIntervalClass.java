package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.time.LocalDate;
import java.time.LocalTime;

import static lsfusion.base.DateConverter.epochToLocalDateTime;
import static lsfusion.base.DateConverter.localDateTimeToUTCEpoch;

public class TimeIntervalClass extends IntervalClass {

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

    @Override
    protected Long parse(String date) {
        return localDateTimeToUTCEpoch(LocalTime.parse(date, TIME_FORMATTER).atDate(LocalDate.now()));
    }

    @Override
    protected String format(Long epoch) {
        return epochToLocalDateTime(epoch).toLocalTime().format(TIME_FORMATTER);
    }
}
