package lsfusion.client.classes.data;

import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.FormatPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.view.MainFrame;

import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public abstract class ClientIntervalClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {

    @Override
    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new FormatPropertyRenderer(property){};
    }

    @Override
    public Object parseString(String s) throws ParseException {
        throw new ParseException("Doesnt support convertation", 0);
    }

    public static Date getDateFromInterval(Object o, boolean from) {
        String object = String.valueOf(o);
        int indexOfDecimal = object.indexOf(".");
        String dateValue = from ? object.substring(0, indexOfDecimal) : object.substring(indexOfDecimal + 1);

        return Date.from(Instant.ofEpochSecond(Long.parseLong(dateValue)));
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

}
