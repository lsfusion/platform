package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

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
    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    @Override
    public String formatString(BigDecimal value) {
        return getLocalDateTime(value, true).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))
                + " - " + getLocalDateTime(value, false).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
    }

    @Override
    public byte getTypeID() {
        return DataType.TIMEINTERVAL;
    }

    @Override
    public Object getSingleValue(BigDecimal value, boolean from) {
        return getLocalDateTime(value, from).atZone(ZoneId.systemDefault()).toLocalTime();
    }
}
