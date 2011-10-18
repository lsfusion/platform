package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.IntegerPropertyEditor;
import platform.gwt.view.classes.GIntegerType;
import platform.gwt.view.classes.GType;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;

public class ClientIntegerClass extends ClientIntegralClass implements ClientTypeClass {

    public final static ClientIntegerClass instance = new ClientIntegerClass();

    private final String sID = "IntegerClass";

    @Override
    public String getSID() {
        return sID;
    }

    public Class getJavaClass() {
        return Integer.class;
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

    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new IntegerPropertyEditor(value, (NumberFormat) format, design, getJavaClass());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.integer");
    }

    @Override
    public GType getGwtType() {
        return GIntegerType.instance;
    }
}
