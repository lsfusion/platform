package platform.client.logics.classes;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.logics.ClientCellView;

import java.text.Format;
import java.io.IOException;

public interface ClientType {

    int getMinimumWidth();
    int getPreferredWidth();
    int getMaximumWidth();

    Format getDefaultFormat();

    PropertyRendererComponent getRendererComponent(Format format);

    abstract public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException;
    abstract public PropertyEditorComponent getClassComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException;
}
