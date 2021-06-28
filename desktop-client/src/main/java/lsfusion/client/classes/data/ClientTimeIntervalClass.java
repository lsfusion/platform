package lsfusion.client.classes.data;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.IntervalPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.table.view.CellTableInterface;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

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
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, CellTableInterface table) {
        return new IntervalPropertyEditor(value, false, this);
    }

    @Override
    public String getIntervalType() {
        return "TIME";
    }

    @Override
    protected Long parse(String date) {
        return localDateTimeToUTCEpoch(LocalTime.parse(date, MainFrame.timeFormatter).atDate(LocalDate.now()));
    }

    @Override
    protected String format(Long epoch) {
        return epochToLocalDateTime(epoch).toLocalTime().format(MainFrame.timeFormatter);
    }
}
