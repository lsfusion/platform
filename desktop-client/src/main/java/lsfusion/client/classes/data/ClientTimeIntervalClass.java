package lsfusion.client.classes.data;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.IntervalPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static lsfusion.base.DateConverter.localDateTimeToUTCEpoch;
import static lsfusion.base.TimeConverter.localTimeToSqlTime;

public class ClientTimeIntervalClass extends ClientIntervalClass {

    public final static ClientTimeIntervalClass instance = new ClientTimeIntervalClass();

    @Override
    public byte getTypeId() {
        return DataType.TIMEINTERVAL;
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new IntervalPropertyEditor(value, false, this);
    }

    @Override
    public String getIntervalType() {
        return "TIME";
    }

    @Override
    protected Long parse(String date) {
        return localDateTimeToUTCEpoch(LocalTime.parse(date, DateTimeFormatter.ofPattern(((SimpleDateFormat) MainFrame.timeFormat).toPattern())).atDate(LocalDate.now()));
    }

    @Override
    protected String format(Long epoch) {
        return MainFrame.timeFormat.format(localTimeToSqlTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.of("UTC")).toLocalTime()));
    }
}
