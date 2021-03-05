package lsfusion.client.classes.data;

import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.DateTimeIntervalPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.DateTimeIntervalPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.awt.*;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ClientDateTimeIntervalClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {

    public final static ClientDateTimeIntervalClass instance = new ClientDateTimeIntervalClass();

    @Override
    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DateTimeIntervalPropertyRenderer(property);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        throw new ParseException("Doesnt support convertation", 0);
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return MainFrame.getDateTimeIntervalDefaultFormat(obj).toString();
    }

    @Override
    public byte getTypeId() {
        return DataType.INTERVAL;
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DateTimeIntervalPropertyEditor(value, property);
    }

    @Override
    public Format getDefaultFormat() {
        return MainFrame.dateTimeIntervalFormat;
    }

    @Override
    public SimpleDateFormat createUserFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    @Override
    protected Object getDefaultWidthValue() {
        return MainFrame.wideFormattableDateTimeInterval;
    }

    @Override
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        int result = super.getFullWidthString(widthString, fontMetrics, propertyDraw);
        if(propertyDraw.isEditableChangeAction())
            result += MainFrame.getIntUISize(18);
        return result;
    }
}
