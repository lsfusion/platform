package lsfusion.gwt.shared.classes;

import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.shared.form.design.GFont;
import lsfusion.gwt.shared.form.design.GFontMetrics;
import lsfusion.gwt.shared.form.design.GWidthStringProcessor;
import lsfusion.gwt.shared.form.property.GPropertyDraw;
import lsfusion.gwt.shared.form.property.cell.GEditBindingMap;
import lsfusion.gwt.shared.form.object.GGroupObjectValue;
import lsfusion.gwt.shared.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.GridCellRenderer;
import lsfusion.gwt.client.form.property.panel.DataPanelRenderer;
import lsfusion.gwt.client.form.property.panel.PanelRenderer;

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
