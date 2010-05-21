package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.DatePropertyRenderer;
import platform.client.form.editor.DatePropertyEditor;
import platform.interop.CellDesign;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;

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

    public PropertyRendererComponent getRendererComponent(Format format, String caption, CellDesign design) { return new DatePropertyRenderer(format, design); }
    public PropertyEditorComponent getComponent(Object value, Format format, CellDesign design) { return new DatePropertyEditor(value, (SimpleDateFormat) format, design); }

}
