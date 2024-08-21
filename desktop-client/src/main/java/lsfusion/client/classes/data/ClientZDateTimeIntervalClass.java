package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.interop.classes.DataType;

import java.text.ParseException;
import java.time.Instant;

public class ClientZDateTimeIntervalClass extends ClientIntervalClass {

    public final static ClientZDateTimeIntervalClass instance = new ClientZDateTimeIntervalClass();

    @Override
    public byte getTypeId() {
        return DataType.ZDATETIMEINTERVAL;
    }

    @Override
    public String getIntervalType() {
        return "ZDATETIME";
    }

    @Override
    protected Long parse(String date) throws ParseException {
        return ClientZDateTimeClass.instance.parseString(date).toEpochMilli();
    }

    @Override
    protected String format(Long epoch) {
        return ClientZDateTimeClass.instance.formatString(Instant.ofEpochMilli(epoch));
    }

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.date.with.time.with.zone.interval");
    }
}
