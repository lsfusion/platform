package lsfusion.client.classes.data;

import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.IntervalPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.FormatPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.view.MainFrame;

import java.awt.*;
import java.math.BigDecimal;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.function.Function;

public abstract class ClientIntervalClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {

    public static ClientIntervalClass getInstance(String type) {
        switch (type) {
            case "DATE":
                return ClientDateIntervalClass.instance;
            case "TIME":
                return ClientTimeIntervalClass.instance;
            case "DATETIME":
                return ClientDateTimeIntervalClass.instance;
            case "ZDATETIME":
                return ClientZDateTimeIntervalClass.instance;
        }
        return null;
    }

    public abstract String getIntervalType();

    @Override
    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new FormatPropertyRenderer(property){};
    }

    @Override
    public Object parseString(String s) throws ParseException {
        throw new ParseException("Doesnt support convertation", 0);
    }

    public static Long getIntervalPart(Object o, boolean from) {
        String object = String.valueOf(o);
        int indexOfDecimal = object.indexOf(".");
        String intervalPart = indexOfDecimal < 0 ? object : from ? object.substring(0, indexOfDecimal) : object.substring(indexOfDecimal + 1);

        return Long.parseLong(intervalPart);
    }

    @Override
    protected Object getDefaultWidthValue() {
        return MainFrame.wideFormattableDateTimeInterval;
    }

    @Override
    public SimpleDateFormat createUserFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    @Override
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        return super.getFullWidthString(widthString, fontMetrics, propertyDraw) + MainFrame.getIntUISize(18);
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return getDefaultFormat(obj).toString();
    }

    public abstract StringBuffer getDefaultFormat(Object o);

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new IntervalPropertyEditor(value, true, this, this::getDefaultFormat);
    }

    public static class IntervalFormat extends Format {
        private final Function<Object, StringBuffer> format;

        public IntervalFormat(Function<Object, StringBuffer> format) {
            this.format = format;
        }

        @Override
        public StringBuffer format(Object o, StringBuffer stringBuffer, FieldPosition fieldPosition) {
            if (o instanceof BigDecimal)
                return format.apply(o);

            return null;
        }

        @Override
        public Object parseObject(String s, ParsePosition parsePosition) {
            return null;
        }
    }

    private IntervalFormat intervalFormat = null;

    @Override
    public Format getDefaultFormat() {
        if (intervalFormat == null)
            intervalFormat = new IntervalFormat(this::getDefaultFormat);

        return intervalFormat;
    }

    public abstract Long parseDateString(String date) throws ParseException;
}
