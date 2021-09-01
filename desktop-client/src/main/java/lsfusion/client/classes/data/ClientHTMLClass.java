package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.HTMLPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientHTMLClass extends ClientStaticFormatFileClass {

    public final static ClientHTMLClass instance = new ClientHTMLClass(false, false);

    public ClientHTMLClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"html"};
    }

    @Override
    public String getDescription() {
        return ClientResourceBundle.getString("logics.classes.html");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new HTMLPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "HTML";
    }

    public byte getTypeId() {
        return DataType.HTML;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.html.file");
    }
}
