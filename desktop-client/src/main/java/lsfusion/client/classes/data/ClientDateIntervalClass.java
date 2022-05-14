package lsfusion.client.classes.data;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.text.ParseException;
import java.time.LocalDate;

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
    protected Long parse(String date) throws ParseException {
        return localDateTimeToUTCEpoch(ClientDateClass.instance.parseString(date).atStartOfDay());
    }

    @Override
    protected String format(Long epoch) {
        return ClientDateClass.instance.formatString(epochToLocalDateTime(epoch).toLocalDate());
    }
}
