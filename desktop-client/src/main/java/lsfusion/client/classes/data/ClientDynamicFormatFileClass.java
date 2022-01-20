package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.FilePropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.DynamicFormatFileRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;

public class ClientDynamicFormatFileClass extends ClientFileClass {

    public final static ClientDynamicFormatFileClass instance = new ClientDynamicFormatFileClass(false, false);

    public ClientDynamicFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DynamicFormatFileRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "File";
    }

    public byte getTypeId() {
        return DataType.DYNAMICFORMATFILE;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new FilePropertyEditor(multiple, storeName, false);
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.custom.file");
    }
}