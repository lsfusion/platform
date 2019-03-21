package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.cell.classes.controller.FilePropertyEditor;
import lsfusion.client.form.property.cell.classes.view.CSVPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
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
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.csv"), getExtensions());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.csv.file");
    }
}
