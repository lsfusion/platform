package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.shared.view.*;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.filter.GCompare;
import lsfusion.gwt.form.client.grid.EditManager;
import lsfusion.gwt.form.client.grid.editor.GridCellEditor;
import lsfusion.gwt.form.client.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.shared.view.panel.DataPanelRenderer;
import lsfusion.gwt.form.shared.view.panel.PanelRenderer;

import java.io.Serializable;
import java.text.ParseException;

public abstract class GType implements Serializable {
    public PanelRenderer createPanelRenderer(GFormController form, GPropertyDraw property, GGroupObjectValue columnKey) {
        return new DataPanelRenderer(form, property, columnKey);
    }

    public abstract GridCellRenderer createGridCellRenderer(GPropertyDraw property);

    public abstract GridCellEditor visit(GTypeVisitor visitor);

    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return null;
    }

    public GridCellEditor createValueCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return createGridCellEditor(editManager, editProperty);
    }

    public GCompare getDefaultCompare() {
        return GCompare.EQUALS;
    }

    // добавляет поправку на кнопки и другие элементы
    public abstract int getFullWidthString(String widthString, GFont font, GWidthStringProcessor widthStringProcessor);

    public abstract int getDefaultWidth(GFont font, GPropertyDraw propertyDraw, GWidthStringProcessor widthStringProcessor);
    public int getDefaultHeight(GFont font) {
        return font == null || font.size == null ? 16 : GFontMetrics.getSymbolHeight(font);
    }

    public abstract GCompare[] getFilterCompares();
    public abstract Object parseString(String s, String pattern) throws ParseException;

    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return null;
    }
}
