package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.editor.IntegerPropertyEditor;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.text.NumberFormat;
import java.text.ParseException;

public class ClientLongClass extends ClientIntegralClass implements ClientTypeClass {

    public final static ClientLongClass instance = new ClientLongClass();

    private final String sID = "LongClass";

    @Override
    public String getSID() {
        return sID;
    }

    public byte getTypeId() {
        return Data.LONG;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.long"), 0);
        }
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new IntegerPropertyEditor(value, (NumberFormat) property.getFormat(), property.design, Long.class);
    }

    @Override
    public String formatString(Object obj) {
        return obj.toString();
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.long");
    }
}
