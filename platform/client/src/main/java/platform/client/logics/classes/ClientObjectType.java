package platform.client.logics.classes;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.CellView;
import platform.client.form.cell.TableCellView;
import platform.client.form.editor.ObjectPropertyEditor;
import platform.client.form.renderer.IntegerPropertyRenderer;
import platform.client.logics.ClientCellView;
import platform.interop.CellDesign;

import java.awt.*;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;

public class ClientObjectType implements ClientType {

    public int getMinimumWidth(FontMetrics fontMetrics) {
        return fontMetrics.stringWidth("999 999") + 8;
    }

    public int getPreferredWidth(FontMetrics fontMetrics) {
        return fontMetrics.stringWidth("9 999 999") + 8;
    }

    public int getMaximumWidth(FontMetrics fontMetrics) { 
        return getPreferredWidth(fontMetrics);
    }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, CellDesign design) { return new IntegerPropertyRenderer(format, design); }

    public CellView getPanelComponent(ClientCellView key, ClientForm form) { return new TableCellView(key, form); }

    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format, CellDesign design) throws IOException, ClassNotFoundException {
        return new ObjectPropertyEditor(form, property.createEditorForm(form.remoteForm));
    }

    public PropertyEditorComponent getClassComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException {
        return new ObjectPropertyEditor(form, property.createClassForm(form.remoteForm, (Integer) value));
    }
}
