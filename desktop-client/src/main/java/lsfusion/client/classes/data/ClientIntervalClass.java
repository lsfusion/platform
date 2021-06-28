package lsfusion.client.classes.data;

import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.IntervalPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.FormatPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.CellTableInterface;
import lsfusion.client.view.MainFrame;

import java.awt.*;
import java.math.BigDecimal;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import static lsfusion.base.DateConverter.formatInterval;
import static lsfusion.base.DateConverter.parseInterval;

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

    protected abstract Long parse(String date);
    protected abstract String format(Long epoch);

    @Override
    public Object parseString(String s) throws ParseException {
        return parseInterval(s, this::parse);
    }

    @Override
    public String formatString(Object obj) {
        return formatInterval(obj, this::format);
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
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, CellTableInterface table) {
        return new IntervalPropertyEditor(value, true, this);
    }

    public static class IntervalFormat extends Format {
        private final ClientIntervalClass intervalClass;

        public IntervalFormat(ClientIntervalClass intervalClass) {
            this.intervalClass = intervalClass;
        }

        @Override
        public StringBuffer format(Object o, StringBuffer stringBuffer, FieldPosition fieldPosition) {
            if (o instanceof BigDecimal)
                return new StringBuffer(intervalClass.formatString(o));

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
            intervalFormat = new IntervalFormat(this);

        return intervalFormat;
    }
}
