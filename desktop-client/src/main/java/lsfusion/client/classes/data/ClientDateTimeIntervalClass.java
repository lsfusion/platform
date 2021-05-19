package lsfusion.client.classes.data;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static lsfusion.base.DateConverter.localDateTimeToSqlTimestamp;

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

    public Long parseDateString(String date) throws ParseException {
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(((SimpleDateFormat) MainFrame.dateTimeFormat).toPattern())).toEpochSecond(ZoneOffset.UTC);
    }

    @Override
    public StringBuffer getDefaultFormat(Object o) {
        Timestamp timestampFrom = localDateTimeToSqlTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(getIntervalPart(o, true)), ZoneId.of("UTC")));
        Timestamp timestampTo = localDateTimeToSqlTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(getIntervalPart(o, false)), ZoneId.of("UTC")));
        return new StringBuffer(MainFrame.dateTimeFormat.format(timestampFrom) + " - " + MainFrame.dateTimeFormat.format(timestampTo));
    }
}
