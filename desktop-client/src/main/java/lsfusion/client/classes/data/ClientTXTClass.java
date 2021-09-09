package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.TXTPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientTXTClass extends ClientStaticFormatFileClass {

    public final static ClientTXTClass instance = new ClientTXTClass(false, false);

    public ClientTXTClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"txt"};
    }

    @Override
    public String getDescription() {
        return ClientResourceBundle.getString("logics.classes.txt");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TXTPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "TEXT";
    }

    public byte getTypeId() {
        return DataType.TXT;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.txt.file");
    }
}