package lsfusion.client.classes.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.classes.renderer.PropertyRenderer;
import lsfusion.client.form.property.classes.renderer.link.HTMLLinkPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientHTMLLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientHTMLLinkClass instance = new ClientHTMLLinkClass(false);

    public ClientHTMLLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new HTMLLinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.HTMLLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.html.link");
    }
}
