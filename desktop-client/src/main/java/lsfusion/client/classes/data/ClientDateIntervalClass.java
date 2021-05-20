package lsfusion.client.classes.data;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static lsfusion.base.DateConverter.*;

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

    @Override
    protected Long parse(String date) {
        return localDateTimeToUTCEpoch(LocalDate.parse(date, DateTimeFormatter.ofPattern(((SimpleDateFormat) MainFrame.dateFormat).toPattern())).atStartOfDay());
    }

    @Override
    protected String format(Long epoch) {
        return MainFrame.dateFormat.format(localDateToSqlDate(epochToLocalDateTime(epoch).toLocalDate()));
    }
}
