package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.editor.IntegerPropertyEditor;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.text.NumberFormat;
import java.text.ParseException;

public class ClientIntegerClass extends ClientIntegralClass implements ClientTypeClass {

    public final static ClientIntegerClass instance = new ClientIntegerClass();

    private final String sID = "IntegerClass";

    @Override
    public String getSID() {
        return sID;
    }

    public byte getTypeId() {
        return Data.INTEGER;
    }

    public Object parseString(String s) throws ParseException {
        try {
            if (!"".equals(s))
                return Integer.parseInt(s);
            else return null;
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.integer"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        return obj.toString();
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new IntegerPropertyEditor(value, (NumberFormat) property.getFormat(), property.design, Integer.class);
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.integer");
    }
}
