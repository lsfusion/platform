package lsfusion.client.classes.data.link;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.LinkPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;

public abstract class ClientStaticFormatLinkClass extends ClientLinkClass {

    protected ClientStaticFormatLinkClass(boolean multiple) {
        super(multiple);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new LinkPropertyEditor(property, value);
    }
}