package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;
import java.time.Instant;

import static lsfusion.base.DateConverter.instantToSqlTimestamp;

public class ZDateTimeIntervalClass extends IntervalClass{

    public final static IntervalClass instance = new ZDateTimeIntervalClass();

    private ZDateTimeIntervalClass() {
        super(LocalizedString.create("{classes.zoned.date.time.interval}"));
    }

    @Override
    public String getSID() {
        return "ZDATETIMEINTERVAL";
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof ZDateTimeIntervalClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.ZDATETIMEINTERVAL;
    }

    @Override
    public BigDecimal getDefaultValue() {
        long l = Instant.now().toEpochMilli() / 1000;
        return new BigDecimal(l + "." + l);
    }

    @Override
    protected Long parse(String date) throws ParseException {
        try {
            return DATE_TIME_FORMAT.parse(date).getTime() / 1000;
        } catch (java.text.ParseException e) {
            throw new ParseException(e.getMessage());
        }
    }

    @Override
    protected String format(Long epoch) {
        return DATE_TIME_FORMAT.format(instantToSqlTimestamp(Instant.ofEpochSecond(epoch)));
    }
}
