package lsfusion.server.logics.classes.data.time;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.time.Instant;

public class ZDateTimeIntervalClass extends IntervalClass<Instant> {

    public final static IntervalClass instance = new ZDateTimeIntervalClass();

    private ZDateTimeIntervalClass() {
        super(LocalizedString.create("{classes.date.with.time.with.zone.interval}"));
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
    protected Long parse(String date) throws lsfusion.server.logics.classes.data.ParseException {
        return ZDateTimeClass.instance.parseInterval(date).toEpochMilli();
    }

    @Override
    protected Long parseUIString(String date) throws lsfusion.server.logics.classes.data.ParseException {
        return ZDateTimeClass.instance.parseIntervalUI(date).toEpochMilli();
    }

    @Override
    protected String format(Long epoch) {
        return ZDateTimeClass.instance.formatInterval(Instant.ofEpochMilli(epoch));
    }

    @Override
    protected String formatUI(Long epoch) {
        return ZDateTimeClass.instance.formatIntervalUI(Instant.ofEpochMilli(epoch));
    }

    @Override
    protected String getSQLFrom(String source) {
        return "to_timestamp((trunc (" + source + "::NUMERIC)) / 1000)";
    }

    @Override
    protected String getSQLTo(String source) {
        return "to_timestamp((SPLIT_PART(" + source + "::TEXT, '.', 2)::NUMERIC) / 1000)";
    }
}
