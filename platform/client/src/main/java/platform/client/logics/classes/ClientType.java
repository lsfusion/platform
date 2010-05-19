package platform.client.logics.classes;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.logics.ClientCellView;

import java.text.Format;
import java.io.IOException;
import java.awt.*;

public interface ClientType {

    int getMinimumWidth(FontMetrics fontMetrics);
    int getPreferredWidth(FontMetrics fontMetrics);
    int getMaximumWidth(FontMetrics fontMetrics);

    Format getDefaultFormat();

    PropertyRendererComponent getRendererComponent(Format format, String caption, Font font);

    abstract public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format, Font font) throws IOException, ClassNotFoundException;
    abstract public PropertyEditorComponent getClassComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException;
}
