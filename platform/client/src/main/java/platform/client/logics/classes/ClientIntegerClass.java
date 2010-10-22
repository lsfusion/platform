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

public class ClientIntegerClass extends ClientIntegralClass implements ClientTypeClass {

    public final static ClientIntegerClass instance = new ClientIntegerClass();

    public Class getJavaClass() {
        return Integer.class;
    }

    public ClientTypeClass getTypeClass() {
        return this;
    }

    public ClientClass getDefaultClass() {
        return this;
    }

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
