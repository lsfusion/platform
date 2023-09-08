package lsfusion.client.classes.data.link;

import lsfusion.base.BaseUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.link.LinkPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientCustomStaticFormatLinkClass extends ClientStaticFormatLinkClass {

    public final String filterDescription;
    public final String[] filterExtensions;

    public ClientCustomStaticFormatLinkClass(String filterDescription, String[] filterExtensions, boolean multiple) {
        super(multiple);
        this.filterDescription = filterDescription;
        this.filterExtensions = filterExtensions;
    }

    public byte getTypeId() {
        return DataType.CUSTOMSTATICFORMATLINK;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new LinkPropertyRenderer(property);
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.static.format.file", BaseUtils.toString(",", filterExtensions));
    }

    public static ClientCustomStaticFormatLinkClass deserialize(DataInputStream inStream) throws IOException {
        Boolean multiple = inStream.readBoolean();
        String filterDescription = inStream.readUTF();
        String[] filterExtensions;
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

        return new ClientCustomStaticFormatLinkClass(filterDescription, filterExtensions, multiple);
    }
}