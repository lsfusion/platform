package lsfusion.client.classes.data.link;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.LinkPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.table.view.CellTableInterface;

public abstract class ClientStaticFormatLinkClass extends ClientLinkClass {

    protected ClientStaticFormatLinkClass(boolean multiple) {
        super(multiple);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, CellTableInterface table) {
        return new LinkPropertyEditor(property, value);
    }
}