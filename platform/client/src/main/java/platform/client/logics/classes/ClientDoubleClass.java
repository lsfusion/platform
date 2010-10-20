package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.DoublePropertyEditor;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.*;

public class ClientDoubleClass extends ClientIntegralClass {

    public ClientDoubleClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public byte getTypeId() {
        return Data.DOUBLE;
    }

    public Class getJavaClass() {
        return Double.class;
    }

    public Format getDefaultFormat() {
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance();
        format.setMaximumFractionDigits(10);
        DecimalFormatSymbols dfs = format.getDecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        format.setDecimalFormatSymbols(dfs);
        return format;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + "не может быть конвертированно в Double.", 0);
        }
    }

    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new DoublePropertyEditor(value, (NumberFormat) format, design, getJavaClass());
    }

    @Override
    public String toString() {
        return "Вещественное число";
    }
}
