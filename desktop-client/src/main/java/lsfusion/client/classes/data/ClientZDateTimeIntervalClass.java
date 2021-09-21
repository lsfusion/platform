package lsfusion.client.classes.data;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.text.ParseException;

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
    protected Long parse(String date) {
        try {
            return MainFrame.dateTimeFormat.parse(date).getTime() / 1000;
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    protected String format(Long epoch) {
        return MainFrame.dateTimeFormat.format(epoch * 1000);
    }
}
