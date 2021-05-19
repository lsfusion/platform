package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static lsfusion.base.DateConverter.localDateTimeToUTCEpoch;
import static lsfusion.base.DateConverter.localDateToSqlDate;

public class DateIntervalClass extends IntervalClass {

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
    protected Long parse(String date) throws ParseException {
        return localDateTimeToUTCEpoch(LocalDate.parse(date, DateTimeFormatter.ofPattern(((SimpleDateFormat) DATE_FORMAT).toPattern())).atStartOfDay());
    }

    @Override
    protected String format(Long epoch) {
        return DATE_FORMAT.format(localDateToSqlDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.of("UTC")).toLocalDate()));
    }

    @Override
    public byte getTypeID() {
        return DataType.DATEINTERVAL;
    }
}
