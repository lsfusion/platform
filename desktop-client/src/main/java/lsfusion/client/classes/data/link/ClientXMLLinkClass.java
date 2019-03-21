package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.link.XMLLinkPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

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