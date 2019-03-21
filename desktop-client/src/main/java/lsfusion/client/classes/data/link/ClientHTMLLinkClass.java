package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.cell.classes.view.link.HTMLLinkPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.classes.DataType;

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
