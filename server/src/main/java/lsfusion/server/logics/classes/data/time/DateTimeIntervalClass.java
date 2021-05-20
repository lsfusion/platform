package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.time.LocalDateTime;

import static lsfusion.base.DateConverter.*;

public class DateTimeIntervalClass extends IntervalClass {

    public final static IntervalClass instance = new DateTimeIntervalClass();

    private DateTimeIntervalClass() {
        super(LocalizedString.create("{classes.date.time.interval}"));
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
    protected Long parse(String date) {
        return localDateTimeToUTCEpoch(LocalDateTime.parse(date, DATE_TIME_FORMATTER));
    }

    @Override
    protected String format(Long epoch) {
        return epochToLocalDateTime(epoch).format(DATE_TIME_FORMATTER);
    }
}
