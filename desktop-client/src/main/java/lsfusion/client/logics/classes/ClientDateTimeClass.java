package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.DateTimePropertyEditor;
import lsfusion.client.form.renderer.DateTimePropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.util.Date;

import static lsfusion.base.DateConverter.createDateTimeEditFormat;
import static lsfusion.base.DateConverter.dateToStamp;
import static lsfusion.client.Main.*;
import static lsfusion.client.form.EditBindingMap.EditEventFilter;

public class ClientDateTimeClass extends ClientDataClass implements ClientTypeClass {
    public final static ClientDateTimeClass instance = new ClientDateTimeClass();

    private final static String sID = "DateTimeClass";

    @Override
    public String getSID() {
        return sID;
    }

    public byte getTypeId() {
        return Data.DATETIME;
    }

    @Override
    public String getPreferredMask() {
        return dateTimeEditFormat.format(wideFormattableDateTime) + "BTN";
    }

    public Format getDefaultFormat() {
        return dateTimeFormat;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DateTimePropertyRenderer(property);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DateTimePropertyEditor(value, createDateTimeEditFormat((DateFormat) property.getFormat()), property.design);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return dateToStamp((Date) dateTimeFormat.parseObject(s));
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.date"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        if (obj != null) {
            return dateTimeFormat.format(obj);
        }
        else return "";
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.date.with.time");
    }

    @Override
    public EditEventFilter getEditEventFilter() {
        return ClientIntegralClass.numberEditEventFilter;
    }
}
