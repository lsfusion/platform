package lsfusion.client.classes.data;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static lsfusion.base.DateConverter.*;

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
        return localDateTimeToUTCEpoch(LocalDateTime.parse(date, DateTimeFormatter.ofPattern(((SimpleDateFormat) MainFrame.dateTimeFormat).toPattern())));
    }

    @Override
    protected String format(Long epoch) {
        return MainFrame.dateTimeFormat.format(localDateTimeToSqlTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.of("UTC"))));
    }
}
