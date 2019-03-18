package lsfusion.client.logics.classes.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.classes.renderer.PropertyRenderer;
import lsfusion.client.form.property.classes.renderer.link.XMLLinkPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientXMLLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientXMLLinkClass instance = new ClientXMLLinkClass(false);

    public ClientXMLLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new XMLLinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.XMLLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.xml.link");
    }
}