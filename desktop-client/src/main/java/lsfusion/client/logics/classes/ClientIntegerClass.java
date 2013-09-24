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

    @Override
    public NumberFormat getDefaultFormat() {
        NumberFormat format = super.getDefaultFormat();
        format.setParseIntegerOnly(true);
        return format;
    }

    @Override
    public Object parseString(String s) throws ParseException {
        try {
            Number n = parseWithDefaultFormat(s);
            long val = n.longValue();
            if (val > (long)Integer.MAX_VALUE) {
                throw new NumberFormatException("Integer " + val + " is out of range");
            }
            return (int)val;
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.double"), 0);
        }
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new IntegerPropertyEditor(value, (NumberFormat) property.getFormat(), property.design, Integer.class);
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.integer");
    }
}
