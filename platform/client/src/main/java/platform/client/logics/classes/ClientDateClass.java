package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.DatePropertyEditor;
import platform.client.form.renderer.DatePropertyRenderer;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ClientDateClass extends ClientDataClass {

    public ClientDateClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public byte getTypeId() {
        return Data.DATE;
    }

    @Override
    public String getPreferredMask() {
        return "01.01.2001"; // пока так, хотя надо будет переделать в зависимости от Locale  
    }

    public Format getDefaultFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new DatePropertyRenderer(format, design);
    }

    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new DatePropertyEditor(value, (SimpleDateFormat) format, design); 
    }

    public Object parseString(String s) throws ParseException {
        try {
            return new SimpleDateFormat().parse(s);
        } catch (Exception e) {
            throw new ParseException(s + "не может быть конвертированно в Date.", 0);
        }
    }

    @Override
    public String toString() {
        return "Дата";
    }
}
