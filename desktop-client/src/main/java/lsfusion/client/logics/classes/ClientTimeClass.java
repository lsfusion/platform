package lsfusion.client.logics.classes;

import lsfusion.base.DateConverter;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.TimePropertyEditor;
import lsfusion.client.form.renderer.TimePropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.sql.Time;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.util.Date;

import static lsfusion.client.Main.*;
import static lsfusion.client.form.EditBindingMap.EditEventFilter;

public class ClientTimeClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientTimeClass instance = new ClientTimeClass();

    private final static String sID = "TimeClass";

    public String getPreferredMask() {
        return timeEditFormat.format(wideFormattableDateTime) + "BT";
    }

    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new TimePropertyEditor(value, DateConverter.createTimeEditFormat((DateFormat) property.getFormat()), property.design);
    }

    public String getSID() {
        return sID;
    }

    public Format getDefaultFormat() {
        return timeFormat;
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
        return Data.TIME;
    }

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.time");
    }

    @Override
    public EditEventFilter getEditEventFilter() {
        return ClientIntegralClass.numberEditEventFilter;
    }
}
