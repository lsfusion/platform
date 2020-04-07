package lsfusion.gwt.client.classes;

import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GWidthStringProcessor;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;
import lsfusion.gwt.client.form.property.panel.view.DataPanelRenderer;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;

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
    
    public int getDefaultCharHeight() {
        return 1;
    }

    public abstract GCompare[] getFilterCompares();
    public abstract Object parseString(String s, String pattern) throws ParseException;

    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return null;
    }
}
