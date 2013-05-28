package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditor;
import platform.client.form.editor.IntegerPropertyEditor;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

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
