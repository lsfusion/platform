package lsfusion.gwt.client.classes;

import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.design.GFontWidthString;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;
import lsfusion.gwt.client.form.property.panel.view.PropertyPanelRenderer;

import java.io.Serializable;
import java.text.ParseException;

public abstract class GType implements Serializable {
    public PanelRenderer createPanelRenderer(GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, LinearCaptionContainer captionContainer) {
        return new PropertyPanelRenderer(form, controller, property, columnKey, captionContainer);
    }

    public abstract CellRenderer createGridCellRenderer(GPropertyDraw property);

    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return null;
    }

    public GCompare getDefaultCompare() {
        return GCompare.EQUALS;
    }

    public static int getFullWidthString(String widthString, GFont font) {
        GFontWidthString fontWidthString = new GFontWidthString(font == null ? GFont.DEFAULT_FONT : font, widthString);
        return GFontMetrics.getStringWidth(fontWidthString); //  + StyleDefaults.CELL_HORIZONTAL_PADDING * 2; min-width doesnt' include padding, so we don't need to add it
    }

    public abstract int getDefaultWidth(GFont font, GPropertyDraw propertyDraw);
    
    public int getDefaultCharHeight() {
        return 1;
    }

    public abstract GCompare[] getFilterCompares();
    public abstract Object parseString(String s, String pattern) throws ParseException;

    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return null;
    }

    public boolean isId(){
        return false;
    }

    public GType getFilterType() {
        return this;
    }
}
