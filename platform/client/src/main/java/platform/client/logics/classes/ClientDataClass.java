package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.cell.CellView;
import platform.client.form.cell.TableCellView;
import platform.client.logics.ClientCellView;
import platform.interop.CellDesign;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.awt.*;

public abstract class ClientDataClass extends ClientClass implements ClientType {

    ClientDataClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public boolean hasChildren() {
        return false;
    }

    public int getMinimumWidth(FontMetrics fontMetrics) {
        return fontMetrics.stringWidth(getMinimumMask()) + 8;
    }
    public int getPreferredWidth(FontMetrics fontMetrics) {
        return fontMetrics.stringWidth(getPreferredMask()) + 8;
    }
    public int getMaximumWidth(FontMetrics fontMetrics) {
        return Integer.MAX_VALUE;
    }

    public String getMinimumMask() {
        return getPreferredMask();
    }
    
    abstract public String getPreferredMask();

    protected abstract PropertyEditorComponent getComponent(Object value, Format format, CellDesign design);

    public CellView getPanelComponent(ClientCellView key, ClientForm form) { return new TableCellView(key, form); }

    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format, CellDesign design) throws IOException, ClassNotFoundException {
        return getComponent(value, format, design);
    }

    public PropertyEditorComponent getClassComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException {
        return getComponent(value, format, null);
    }
}
