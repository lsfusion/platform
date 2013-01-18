package platform.gwt.form.shared.view.classes;

import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.filter.GCompare;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.StringGridCellRenderer;
import platform.gwt.form.shared.view.panel.DataPanelRenderer;
import platform.gwt.form.shared.view.panel.PanelRenderer;

import java.io.Serializable;

public abstract class GType implements Serializable {
    public Object parseString(String strValue) {
        return strValue;
    }

    public PanelRenderer createPanelRenderer(GFormController form, GPropertyDraw property, GGroupObjectValue columnKey) {
        return new DataPanelRenderer(form, property, columnKey);
    }

    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new StringGridCellRenderer(property);
    }

    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return null;
    }

    public GridCellEditor createValueCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return createGridCellEditor(editManager, editProperty);
    }

    public GCompare getDefaultCompare() {
        return GCompare.EQUALS;
    }

    public abstract int getMinimumPixelWidth(int minimumCharWidth, GFont font);
    public abstract int getMaximumPixelWidth(int maximumCharWidth, GFont font);
    public abstract int getPreferredPixelWidth(int preferredCharWidth, GFont font);
    public abstract GCompare[] getFilterCompares();

    public int getMinimumPixelHeight(GFont font) {
        return font == null || font.size == null ? 16 : (int) (font.size * 1.25);
    }

    public int getPreferredPixelHeight(GFont font) {
        return getMinimumPixelHeight(font);
    }
}
