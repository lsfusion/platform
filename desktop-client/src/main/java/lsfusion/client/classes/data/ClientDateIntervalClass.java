package lsfusion.client.classes.data;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static lsfusion.base.DateConverter.localDateToSqlDate;

public class ClientDateIntervalClass extends ClientIntervalClass {

    public final static ClientIntervalClass instance = new ClientDateIntervalClass();
    @Override
    public byte getTypeId() {
        return DataType.DATEINTERVAL;
    }

    @Override
    public String getIntervalType() {
        return "DATE";
    }

    public Long parseDateString(String date) throws ParseException {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern(((SimpleDateFormat) MainFrame.dateFormat).toPattern())).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
    }

    @Override
    public StringBuffer getDefaultFormat(Object o) {
        Date dateFrom = localDateToSqlDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(getIntervalPart(o, true)), ZoneId.of("UTC")).toLocalDate());
        Date dateTo = localDateToSqlDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(getIntervalPart(o, false)), ZoneId.of("UTC")).toLocalDate());
        return new StringBuffer(MainFrame.dateFormat.format(dateFrom) + " - " + MainFrame.dateFormat.format(dateTo));
    }
}
