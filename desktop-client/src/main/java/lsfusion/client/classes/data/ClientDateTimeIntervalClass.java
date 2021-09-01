package lsfusion.client.classes.data;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

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
    protected Long parse(String date) {
        return localDateTimeToUTCEpoch(LocalDateTime.parse(date, MainFrame.dateTimeFormatter));
    }

    @Override
    protected String format(Long epoch) {
        return epochToLocalDateTime(epoch).format(MainFrame.dateTimeFormatter);
    }
}
