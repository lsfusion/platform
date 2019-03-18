package lsfusion.client.classes.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.classes.renderer.PropertyRenderer;
import lsfusion.client.form.property.classes.renderer.link.ImageLinkPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientImageLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientImageLinkClass instance = new ClientImageLinkClass(false);

    public ClientImageLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeId() {
        return DataType.IMAGELINK;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new ImageLinkPropertyRenderer(property);
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.image.link");
    }
}