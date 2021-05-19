package lsfusion.client.classes.data;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.IntervalPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static lsfusion.base.TimeConverter.localTimeToSqlTime;

public class ClientTimeIntervalClass extends ClientIntervalClass {

    public final static ClientTimeIntervalClass instance = new ClientTimeIntervalClass();

    @Override
    public byte getTypeId() {
        return DataType.TIMEINTERVAL;
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new IntervalPropertyEditor(value, false, this, this::getDefaultFormat);
    }

    @Override
    public String getIntervalType() {
        return "TIME";
    }

    public Long parseDateString(String date) throws ParseException {
        return LocalTime.parse(date, DateTimeFormatter.ofPattern(((SimpleDateFormat) MainFrame.timeFormat).toPattern())).atDate(LocalDate.now()).toEpochSecond(ZoneOffset.UTC);
    }

    @Override
    public StringBuffer getDefaultFormat(Object o) {
        Time timeFrom = localTimeToSqlTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(getIntervalPart(o, true)), ZoneId.of("UTC")).toLocalTime());
        Time timeTo = localTimeToSqlTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(getIntervalPart(o, false)), ZoneId.of("UTC")).toLocalTime());
        return new StringBuffer(MainFrame.timeFormat.format(timeFrom) + " - " + MainFrame.timeFormat.format(timeTo));
    }
}
