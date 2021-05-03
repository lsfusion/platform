package lsfusion.client.classes.data;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.FilePropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;

public abstract class ClientStaticFormatFileClass extends ClientFileClass {

    protected ClientStaticFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }
    
    public String getExtension() { // should be equal to StaticFormatFileClass.getExtension
        return getExtensions()[0];
    }

    public abstract String[] getExtensions();

    public abstract String getDescription();

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, getDescription(), getExtensions());
    }
}
