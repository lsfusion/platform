package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

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
    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    @Override
    public String formatString(BigDecimal value) {
        return getLocalDateTime(value, true).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM))
                + " - " + getLocalDateTime(value, false).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM));
    }

    @Override
    public byte getTypeID() {
        return DataType.DATETIMEINTERVAL;
    }
}
