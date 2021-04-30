package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.XMLPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientXMLClass extends ClientStaticFormatFileClass {

    public final static ClientXMLClass instance = new ClientXMLClass(false, false);

    public ClientXMLClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"xml"};
    }

    @Override
    public String getDescription() {
        return ClientResourceBundle.getString("logics.classes.xml");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new XMLPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "XML";
    }

    public byte getTypeId() {
        return DataType.XML;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.xml.file");
    }
}
