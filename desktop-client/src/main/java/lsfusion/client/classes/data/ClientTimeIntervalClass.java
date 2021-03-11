package lsfusion.client.classes.data;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.TimeIntervalPropertyEditor;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.math.BigDecimal;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;

public class ClientTimeIntervalClass extends ClientIntervalClass {

    public final static ClientTimeIntervalClass instance = new ClientTimeIntervalClass();

    @Override
    public byte getTypeId() {
        return DataType.TIMEINTERVAL;
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return getTimeIntervalDefaultFormat(obj).toString();
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new TimeIntervalPropertyEditor(value, property);
    }

    @Override
    public Format getDefaultFormat() {
        return MainFrame.timeIntervalFormat;
    }

    public static class TimeIntervalFormat extends Format {
        @Override
        public StringBuffer format(Object o, StringBuffer stringBuffer, FieldPosition fieldPosition) {
            if (o instanceof BigDecimal) {
                return getTimeIntervalDefaultFormat(o);
            }
            return null;
        }

        @Override
        public Object parseObject(String s, ParsePosition parsePosition) {
            return null;
        }
    }

    public static StringBuffer getTimeIntervalDefaultFormat(Object o) {
        return new StringBuffer(MainFrame.timeFormat.format(getDateFromInterval(o, true))
                + " - " + MainFrame.timeFormat.format(getDateFromInterval(o, false)));
    }
}
