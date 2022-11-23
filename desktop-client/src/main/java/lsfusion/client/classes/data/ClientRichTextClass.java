package lsfusion.client.classes.data;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.rich.RichTextPropertyEditor;
import lsfusion.client.form.property.cell.classes.view.TextPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;

import java.awt.*;

public class ClientRichTextClass extends ClientTextClass {

    public ClientRichTextClass() {
        super("rich");
    }

    @Override
    public byte getTypeId() {
        return DataType.RICHTEXT;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TextPropertyRenderer(property, true);
    }

    @Override
    public PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        return new RichTextPropertyEditor(ownerComponent, value, property.design);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new RichTextPropertyEditor(value, property.design);
    }

    @Override
    public boolean trimTooltip() {
        return false;
    }
}
