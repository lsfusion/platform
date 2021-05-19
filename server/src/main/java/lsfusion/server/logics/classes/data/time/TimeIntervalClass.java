package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static lsfusion.base.DateConverter.localDateTimeToUTCEpoch;
import static lsfusion.base.TimeConverter.localTimeToSqlTime;

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
    protected Long parse(String date) throws ParseException {
        return localDateTimeToUTCEpoch(LocalTime.parse(date, DateTimeFormatter.ofPattern(((SimpleDateFormat) TIME_FORMAT).toPattern())).atDate(LocalDate.now()));
    }

    @Override
    protected String format(Long epoch) {
        return TIME_FORMAT.format(localTimeToSqlTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.of("UTC")).toLocalTime()));
    }
}
