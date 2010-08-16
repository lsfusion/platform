package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.FileActionPropertyEditor;
import platform.client.logics.ClientCell;
import platform.interop.ComponentDesign;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientFileActionClass extends ClientActionClass {
    private String filterDescription;
    private String filterExtensions[];

    public ClientFileActionClass(DataInputStream inStream) throws IOException {
        super(inStream);

        filterDescription = inStream.readUTF();
        int extCount = inStream.readInt();
        if (extCount <= 0) {
            filterExtensions = new String[1];
            filterExtensions[0] = "*";
        } else {
            filterExtensions = new String[extCount];

            for (int i = 0; i < extCount; ++i) {
                filterExtensions[i] = inStream.readUTF();
            }
        }
    }

    @Override
    public PropertyEditorComponent getEditorComponent(ClientFormController form, ClientCell property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException {
//         return new FileActionPropertyEditor("Файлы таблиц (*.xls)", "xls");
         return new FileActionPropertyEditor(filterDescription, filterExtensions);
    }
}