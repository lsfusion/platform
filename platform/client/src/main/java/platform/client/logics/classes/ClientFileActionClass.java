package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.FileActionPropertyEditor;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;

public class ClientFileActionClass extends ClientActionClass {
    private String filterDescription;
    private String filterExtensions[];

    private String sID;

    @Override
    public String getSID() {
        return sID;
    }

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
        sID = "FileActionClass[" + filterDescription + "," + filterExtensions + "]";
    }

    @Override
    public byte getTypeId() {
        return Data.FILEACTION;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        //todo:
    }

    @Override
    public PropertyEditorComponent getEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException {
//         return new FileActionPropertyEditor("Файлы таблиц (*.xls)", "xls");
        return new FileActionPropertyEditor(filterDescription, filterExtensions);
    }
}