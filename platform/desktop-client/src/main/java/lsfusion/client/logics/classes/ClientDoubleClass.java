package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.StartupProperties;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.editor.DoublePropertyEditor;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.text.*;

public class ClientDoubleClass extends ClientIntegralClass implements ClientTypeClass {

    public final static ClientDoubleClass instance = new ClientDoubleClass();

    private final String sID = "DoubleClass";

    @Override
    public String getSID() {
        return sID;
    }

    protected ClientDoubleClass() {
    }

    public byte getTypeId() {
        return Data.DOUBLE;
    }

    public Format getDefaultFormat() {
        DecimalFormat format = (DecimalFormat) super.getDefaultFormat();
        format.setMaximumFractionDigits(10);
        DecimalFormatSymbols dfs = format.getDecimalFormatSymbols();
        if ((dfs.getGroupingSeparator() != '.') && StartupProperties.dotSeparator)
            dfs.setDecimalSeparator('.');
        format.setDecimalFormatSymbols(dfs);
        return format;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return NumberFormat.getInstance().parse(s).doubleValue();
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.double"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        return NumberFormat.getInstance().format(obj);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DoublePropertyEditor(value, (NumberFormat) property.getFormat(), property.design, Double.class);
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.real.number");
    }
}
