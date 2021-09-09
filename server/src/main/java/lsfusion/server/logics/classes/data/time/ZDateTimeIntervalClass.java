package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;

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
    protected Long parse(String date) {
        try {
            return ((SimpleDateFormat)Z_DATE_TIME_FORMATTER.toFormat()).parse(date).getTime() / 1000;
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    protected String format(Long epoch) {
        return Z_DATE_TIME_FORMATTER.format(Instant.ofEpochSecond(epoch));
    }
}
