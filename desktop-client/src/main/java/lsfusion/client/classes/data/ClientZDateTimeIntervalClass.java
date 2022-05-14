package lsfusion.client.classes.data;

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
        return ClientZDateTimeClass.instance.parseString(date).getEpochSecond();
    }

    @Override
    protected String format(Long epoch) {
        return ClientZDateTimeClass.instance.formatString(Instant.ofEpochSecond(epoch));
    }
}
