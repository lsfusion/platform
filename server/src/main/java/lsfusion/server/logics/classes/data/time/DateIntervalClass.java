package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;

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
    public String formatString(BigDecimal value) {
        return LOCAL_DATE_TIME.toLocalDate() + " - " + LOCAL_DATE_TIME.toLocalDate();
    }

    @Override
    public byte getTypeID() {
        return DataType.DATEINTERVAL;
    }
}
