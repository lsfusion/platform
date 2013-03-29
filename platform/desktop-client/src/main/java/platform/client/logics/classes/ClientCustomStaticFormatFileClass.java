package platform.client.logics.classes;

import platform.client.form.PropertyEditor;
import platform.client.form.PropertyRenderer;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.form.renderer.CustomStaticFormatFileRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

public class ClientCustomStaticFormatFileClass extends ClientStaticFormatFileClass {

    public String filterDescription;
    public String filterExtensions[];

    public ClientCustomStaticFormatFileClass(DataInputStream inStream) throws IOException {
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
    public String[] getExtensions() {
        return filterExtensions;
    }

    public String getFileSID() {
        throw new RuntimeException("SID overrided");
    }

    private String sID;
    public String getSID() {
        if(sID==null)
            sID = "FileActionClass[" + multiple + ", " + filterDescription + "," + Arrays.toString(filterExtensions) + "]";
        return sID;
    }

    public byte getTypeId() {
        return Data.CUSTOMSTATICFORMATFILE;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        //todo:
    }

    public String formatString(Object obj) throws ParseException {
        return filterDescription;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new CustomStaticFormatFileRenderer(property, filterExtensions[0]);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw design) {
        return new FilePropertyEditor(multiple, storeName, filterDescription, filterExtensions);
    }
}
