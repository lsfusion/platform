package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.DatePropertyRenderer;
import platform.client.form.editor.DatePropertyEditor;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;

public class ClientDateClass extends ClientDataClass {

    public ClientDateClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public int getPreferredWidth() { return 70; }

    public Format getDefaultFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption) { return new DatePropertyRenderer(format); }
    public PropertyEditorComponent getComponent(Object value, Format format) { return new DatePropertyEditor(value, (SimpleDateFormat) format); }

}
