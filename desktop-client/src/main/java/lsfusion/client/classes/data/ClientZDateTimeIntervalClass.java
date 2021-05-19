package lsfusion.client.classes.data;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.text.ParseException;
import java.time.Instant;

import static lsfusion.base.DateConverter.instantToSqlTimestamp;

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
        return MainFrame.dateTimeFormat.parse(date).getTime() / 1000;
    }

    @Override
    protected String format(Long epoch) {
        return MainFrame.dateTimeFormat.format(instantToSqlTimestamp(Instant.ofEpochSecond(epoch)));
    }
}
