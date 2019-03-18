package lsfusion.client.logics.classes;

import lsfusion.base.DateConverter;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.property.classes.editor.TimePropertyEditor;
import lsfusion.client.form.property.classes.renderer.TimePropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

import java.sql.Time;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static lsfusion.client.Main.*;
import static lsfusion.client.form.EditBindingMap.EditEventFilter;

public class ClientTimeClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {

    public final static ClientTimeClass instance = new ClientTimeClass();

    @Override
    protected Object getDefaultWidthValue() {
        return wideFormattableDateTime;
    }

    @Override
    protected SimpleDateFormat getEditFormat(Format format) {
        return DateConverter.createTimeEditFormat((DateFormat)format);
    }

    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new TimePropertyEditor(value, getEditFormat(property), property.design);
    }

    public Format getDefaultFormat() {
        return timeFormat;
    }

    @Override
    public SimpleDateFormat createUserFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TimePropertyRenderer(property);
    }

    public Time parseString(String s) throws ParseException {
        try {
            return new Time(((Date) timeFormat.parseObject(s)).getTime());
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.time"), 0);
        }
    }

    public String formatString(Object obj) {
        if (obj != null) {
            return timeFormat.format(obj);
        }
        else return "";
    }

    public byte getTypeId() {
        return DataType.TIME;
    }

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.time");
    }

    @Override
    public EditEventFilter getEditEventFilter() {
        return ClientIntegralClass.numberEditEventFilter;
    }
}
