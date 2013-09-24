package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.DatePropertyEditor;
import lsfusion.client.form.renderer.DatePropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.sql.Date;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;

import static lsfusion.base.DateConverter.createDateEditFormat;
import static lsfusion.base.DateConverter.safeDateToSql;
import static lsfusion.client.Main.dateFormat;
import static lsfusion.client.form.EditBindingMap.EditEventFilter;

public class ClientDateClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientDateClass instance = new ClientDateClass();

    private final String sID = "DateClass";

    @Override
    public String getSID() {
        return sID;
    }

    public byte getTypeId() {
        return Data.DATE;
    }

    @Override
    public String getPreferredMask() {
        try {
            return formatString(safeDateToSql(new java.util.Date()));
        } catch (ParseException pe) {
            throw new IllegalStateException("shouldn't happen", pe);
        }
    }

    public Format getDefaultFormat() {
        return dateFormat;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DatePropertyRenderer(property);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DatePropertyEditor(value, createDateEditFormat((DateFormat) property.getFormat()), property.design);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return safeDateToSql(dateFormat.parse(s));
        } catch (Exception e) {
            throw new ParseException(s +  ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.date"), 0);
        }
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        if (obj != null) {
            return dateFormat.format((Date) obj);
        }
        else return "";
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.date");
    }

    @Override
    public EditEventFilter getEditEventFilter() {
        return ClientIntegralClass.numberEditEventFilter;
    }
}
