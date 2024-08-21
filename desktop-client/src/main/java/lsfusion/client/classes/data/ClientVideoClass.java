package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.VideoPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientVideoClass extends ClientStaticFormatFileClass {

    public final static ClientVideoClass instance = new ClientVideoClass(false, false);

    public ClientVideoClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"mp4"};
    }

    @Override
    public String getDescription() {
        return ClientResourceBundle.getString("logics.classes.video");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new VideoPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "VIDEO";
    }

    public byte getTypeId() {
        return DataType.VIDEO;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.video.file");
    }
}
