package platform.client.logics.classes;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.editor.ObjectPropertyEditor;
import platform.client.form.renderer.IntegerPropertyRenderer;
import platform.client.logics.ClientCellView;

import java.text.Format;
import java.text.NumberFormat;
import java.io.IOException;

public class ClientObjectType implements ClientType {

    public int getMinimumWidth() { return getPreferredWidth(); }
    public int getPreferredWidth() { return 45; }
    public int getMaximumWidth() { return getPreferredWidth(); }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new IntegerPropertyRenderer(format); }

    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException {
        return new ObjectPropertyEditor(form, property.createEditorForm(form.clientNavigator.remoteNavigator, form.getID()));
    }

    public PropertyEditorComponent getClassComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException {
        return new ObjectPropertyEditor(form, property.createClassForm(form.clientNavigator.remoteNavigator, form.getID(), (Integer) value));
    }
}
