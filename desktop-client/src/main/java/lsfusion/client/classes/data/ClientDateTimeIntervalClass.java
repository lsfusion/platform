package lsfusion.client.classes.data;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.IntervalPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.math.BigDecimal;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

public class ClientDateTimeIntervalClass extends ClientIntervalClass {

    public final static ClientDateTimeIntervalClass instance = new ClientDateTimeIntervalClass();

    @Override
    public byte getTypeId() {
        return DataType.DATETIMEINTERVAL;
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return getDateTimeIntervalDefaultFormat(obj).toString();
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new IntervalPropertyEditor(value, (SimpleDateFormat) MainFrame.dateTimeFormat, true);
    }

    @Override
    public Format getDefaultFormat() {
        return MainFrame.dateTimeIntervalFormat;
    }

    @Override
    public String getIntervalType() {
        return "DATETIME";
    }

    public static class DateTimeIntervalFormat extends Format {
        @Override
        public StringBuffer format(Object o, StringBuffer stringBuffer, FieldPosition fieldPosition) {
            if (o instanceof BigDecimal) {
                return getDateTimeIntervalDefaultFormat(o);
            }
            return null;
        }

        @Override
        public Object parseObject(String s, ParsePosition parsePosition) {
            return null;
        }
    }

    public static StringBuffer getDateTimeIntervalDefaultFormat(Object o) {
        return new StringBuffer(MainFrame.dateTimeFormat.format(getDateFromInterval(o, true))
                + " - " + MainFrame.dateTimeFormat.format(getDateFromInterval(o, false)));
    }
}
