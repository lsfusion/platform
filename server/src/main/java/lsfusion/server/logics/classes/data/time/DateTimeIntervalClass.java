package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;

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
    public String formatString(BigDecimal value) {
        return LOCAL_DATE_TIME + " - " + LOCAL_DATE_TIME;
    }

    @Override
    public byte getTypeID() {
        return DataType.DATETIMEINTERVAL;
    }
}
