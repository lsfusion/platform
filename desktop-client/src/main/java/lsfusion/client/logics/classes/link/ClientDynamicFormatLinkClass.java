package lsfusion.client.logics.classes.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.classes.editor.PropertyEditor;
import lsfusion.client.form.property.classes.renderer.PropertyRenderer;
import lsfusion.client.form.property.classes.editor.LinkPropertyEditor;
import lsfusion.client.form.property.classes.renderer.link.DynamicFormatLinkRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientDynamicFormatLinkClass extends ClientLinkClass {

    public final static ClientDynamicFormatLinkClass instance = new ClientDynamicFormatLinkClass(false);

    public ClientDynamicFormatLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DynamicFormatLinkRenderer(property);
    }

    public byte getTypeId() {
        return DataType.DYNAMICFORMATLINK;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new LinkPropertyEditor(property, value);
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.custom.link");
    }
}