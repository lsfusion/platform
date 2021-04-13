package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

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
    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    @Override
    public String formatString(BigDecimal value) {
        return getLocalDateTime(value, true).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
                + " - " + getLocalDateTime(value, false).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
    }

    @Override
    public byte getTypeID() {
        return DataType.DATEINTERVAL;
    }
}
