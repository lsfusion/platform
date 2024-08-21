package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.link.LinkPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientVideoLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientVideoLinkClass instance = new ClientVideoLinkClass(false);

    public ClientVideoLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new LinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.VIDEOLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.video.link");
    }
}
