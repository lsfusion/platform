package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.TablePropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientTableClass extends ClientStaticFormatFileClass {

    public final static ClientTableClass instance = new ClientTableClass(false, false);

    public ClientTableClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"table"};
    }

    @Override
    public String getDescription() {
        return ClientResourceBundle.getString("logics.classes.table");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TablePropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "table";
    }

    public byte getTypeId() {
        return DataType.TABLE;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.table.file");
    }
}
