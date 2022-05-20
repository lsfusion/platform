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
        return localDateTimeToUTCEpoch(DateClass.instance.parseString(date).atStartOfDay());
    }

    @Override
    protected String format(Long epoch) {
        return DateClass.instance.formatString(epochToLocalDateTime(epoch).toLocalDate());
    }
}
