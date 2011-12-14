package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.CustomFileEditor;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;

public class ClientFileActionClass extends ClientActionClass {

    private boolean multiple;
    private boolean custom;
    private String filterDescription;
    private String filterExtensions[];

    private String sID;

    @Override
    public String getSID() {
        return sID;
    }

    public ClientFileActionClass(DataInputStream inStream) throws IOException {
        super(inStream);

        multiple = inStream.readBoolean();
        custom = inStream.readBoolean();
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
    protected PropertyEditorComponent getComponent(Object value, ClientPropertyDraw property) {
        return null;
    }

    @Override
    public PropertyEditorComponent getEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) throws IOException, ClassNotFoundException {
        return custom ? new CustomFileEditor(value, false, multiple) : new CustomFileEditor(value, false, multiple, filterDescription, filterExtensions);
    }
}