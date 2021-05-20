package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.time.LocalDate;

import static lsfusion.base.DateConverter.*;

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
    public byte getTypeID() {
        return DataType.DATEINTERVAL;
    }

    @Override
    protected Long parse(String date) {
        return localDateTimeToUTCEpoch(LocalDate.parse(date, DATE_FORMATTER).atStartOfDay());
    }

    @Override
    protected String format(Long epoch) {
        return epochToLocalDateTime(epoch).toLocalDate().format(DATE_FORMATTER);
    }
}
