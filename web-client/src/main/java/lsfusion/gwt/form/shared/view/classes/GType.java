package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GFontMetrics;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.filter.GCompare;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.shared.view.panel.DataPanelRenderer;
import lsfusion.gwt.form.shared.view.panel.PanelRenderer;

import java.io.Serializable;
import java.text.ParseException;

public abstract class GType implements Serializable {
    public PanelRenderer createPanelRenderer(GFormController form, GPropertyDraw property, GGroupObjectValue columnKey) {
        return new DataPanelRenderer(form, property, columnKey);
    }

    public abstract GridCellRenderer createGridCellRenderer(GPropertyDraw property);

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
    public abstract Object parseString(String s) throws ParseException;

    public int getMinimumPixelHeight(GFont font) {
        return font == null || font.size == null ? 16 : GFontMetrics.getSymbolHeight(font);
    }

    public int getPreferredPixelHeight(GFont font) {
        return getMinimumPixelHeight(font);
    }

    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return null;
    }
}
