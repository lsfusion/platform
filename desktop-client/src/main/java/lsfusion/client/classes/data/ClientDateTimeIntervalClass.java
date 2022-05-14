package lsfusion.client.classes.data;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.text.ParseException;
import java.time.LocalDateTime;

import static lsfusion.base.DateConverter.*;
import static lsfusion.base.DateConverter.epochToLocalDateTime;

public class ClientDateTimeIntervalClass extends ClientIntervalClass {

    public final static ClientDateTimeIntervalClass instance = new ClientDateTimeIntervalClass();

    @Override
    public byte getTypeId() {
        return DataType.DATETIMEINTERVAL;
    }

    @Override
    public String getIntervalType() {
        return "DATETIME";
    }

    @Override
    protected Long parse(String date) throws ParseException {
        return localDateTimeToUTCEpoch((LocalDateTime) ClientDateTimeClass.instance.parseString(date));
    }

    @Override
    protected String format(Long epoch) {
        return ClientDateTimeClass.instance.formatString(epochToLocalDateTime(epoch));
    }
}
