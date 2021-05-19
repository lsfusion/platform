package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;

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
    public String formatString(BigDecimal value) {
        return LOCAL_DATE_TIME.toLocalTime() + " - " + LOCAL_DATE_TIME.toLocalTime();
    }

    @Override
    public byte getTypeID() {
        return DataType.TIMEINTERVAL;
    }
}
