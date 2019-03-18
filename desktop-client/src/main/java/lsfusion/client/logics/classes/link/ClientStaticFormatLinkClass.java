package lsfusion.client.logics.classes.link;

import lsfusion.client.form.property.classes.editor.PropertyEditor;
import lsfusion.client.form.property.classes.editor.LinkPropertyEditor;
import lsfusion.client.logics.ClientPropertyDraw;

public abstract class ClientStaticFormatLinkClass extends ClientLinkClass {

    protected ClientStaticFormatLinkClass(boolean multiple) {
        super(multiple);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new LinkPropertyEditor(property, value);
    }
}