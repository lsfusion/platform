package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.IntegerPropertyEditor;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;

public class ClientIntegerClass extends ClientIntegralClass {

    public ClientIntegerClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    @Override
    public byte getTypeId() {
        return Data.INTEGER;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + "не может быть конвертированно в Integer.", 0);
        }
    }

    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new IntegerPropertyEditor(value, (NumberFormat) format, design, getJavaClass());
    }

    @Override
    public String toString() {
        return "Целое число";
    }
}
