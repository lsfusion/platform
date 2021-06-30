package lsfusion.client.classes.data.link;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.LinkPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;

public abstract class ClientStaticFormatLinkClass extends ClientLinkClass {

    protected ClientStaticFormatLinkClass(boolean multiple) {
        super(multiple);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new LinkPropertyEditor(property, value);
    }
}