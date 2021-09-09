package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.CSVPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientCSVClass extends ClientStaticFormatFileClass {

    public final static ClientCSVClass instance = new ClientCSVClass(false, false);

    public ClientCSVClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"csv"};
    }

    @Override
    public String getDescription() {
        return ClientResourceBundle.getString("logics.classes.csv");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new CSVPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "CSV";
    }

    public byte getTypeId() {
        return DataType.CSV;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.csv.file");
    }
}
