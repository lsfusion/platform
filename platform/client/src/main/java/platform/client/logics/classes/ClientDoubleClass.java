package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.DoublePropertyEditor;
import platform.client.logics.ClientPropertyDraw;
import platform.gwt.view.classes.GDoubleType;
import platform.gwt.view.classes.GType;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
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

    protected ClientDoubleClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public byte getTypeId() {
        return Data.DOUBLE;
    }

    public Class getJavaClass() {
        return Double.class;
    }

    public Format getDefaultFormat() {
        DecimalFormat format = (DecimalFormat) super.getDefaultFormat();
        format.setMaximumFractionDigits(10);
        DecimalFormatSymbols dfs = format.getDecimalFormatSymbols();
        if (dfs.getGroupingSeparator() != '.')
            dfs.setDecimalSeparator('.');
        format.setDecimalFormatSymbols(dfs);
        return format;
    }

    public String reformatString(String string) {
        DecimalFormat format = (DecimalFormat) getDefaultFormat();
        if (format.getDecimalFormatSymbols().getGroupingSeparator() != '.')
            return string.replaceAll(",", ".");
        else
            return string;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Double.parseDouble(reformatString(s));
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.double"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        return reformatString(obj.toString());
    }

    public PropertyEditorComponent getComponent(Object value, ClientPropertyDraw property) {
        return new DoublePropertyEditor(value, (NumberFormat) property.getFormat(), property.design, getJavaClass());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.real.number");
    }

    @Override
    public GType getGwtType() {
        return GDoubleType.instance;
    }
}
