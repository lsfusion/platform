package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form2.shared.view.grid.renderer.StringGridRenderer;
import platform.gwt.form2.shared.view.panel.DataPanelRenderer;
import platform.gwt.form2.shared.view.panel.PanelRenderer;

import java.io.Serializable;

public abstract class GType implements Serializable {
    public Object parseString(String strValue) {
        return strValue;
    }

    public PanelRenderer createPanelRenderer(GFormController form, GPropertyDraw property) {
        return new DataPanelRenderer(form, property);
    }

    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new StringGridRenderer();
    }

    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return null;
    }
}
