package lsfusion.client.classes.data;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.IntervalPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;

import static lsfusion.base.DateConverter.epochToLocalDateTime;
import static lsfusion.base.DateConverter.localDateTimeToUTCEpoch;

public class ClientTimeIntervalClass extends ClientIntervalClass {

    public final static ClientTimeIntervalClass instance = new ClientTimeIntervalClass();

    @Override
    public byte getTypeId() {
        return DataType.TIMEINTERVAL;
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new IntervalPropertyEditor(value, false, this);
    }

    @Override
    public String getIntervalType() {
        return "TIME";
    }

    // should correspond TimeIntervalClass, GTimeDTO.toTime
    private static final LocalDate dateEpoch = LocalDate.of(1900, 1, 1);
    @Override
    protected Long parse(String date) throws ParseException {
        return localDateTimeToUTCEpoch(ClientTimeClass.instance.parseString(date).atDate(dateEpoch));
    }

    @Override
    protected String format(Long epoch) {
        return ClientTimeClass.instance.formatString(epochToLocalDateTime(epoch).toLocalTime());
    }
}
