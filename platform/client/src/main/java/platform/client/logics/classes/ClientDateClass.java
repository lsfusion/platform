package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.logics.ClientCellView;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;

public class ClientDateClass extends ClientClass {

    public ClientDateClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public int getPreferredWidth() { return 70; }

    public Format getDefaultFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new DatePropertyRenderer(format); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new DatePropertyEditor(value, (SimpleDateFormat) format); }

    public Class getJavaClass() {
        return java.util.Date.class;
    }

}
