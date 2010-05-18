package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.DatePropertyRenderer;
import platform.client.form.editor.DatePropertyEditor;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.awt.*;

public class ClientDateClass extends ClientDataClass {

    public ClientDateClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public String getPreferredMask() {
        return "01.01.2001"; // пока так, хотя надо будет переделать в зависимости от Locale  
    }

    public Format getDefaultFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, Font font) { return new DatePropertyRenderer(format, font); }
    public PropertyEditorComponent getComponent(Object value, Format format) { return new DatePropertyEditor(value, (SimpleDateFormat) format); }

}
