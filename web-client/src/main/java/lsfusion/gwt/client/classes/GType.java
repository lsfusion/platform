package lsfusion.gwt.client.classes;

import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
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

    // not null
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean needNotNull, boolean globalCaptionIsDrawn) {
        String widthString = getDefaultWidthString(propertyDraw);

        return GFontMetrics.getStringWidth(font, widthString);
    }

    public GSize getDefaultHeight(GFont font, GPropertyDraw propertyDraw, boolean needNotNull, boolean globalCaptionIsDrawn) {
        String heightString = getDefaultHeightString(propertyDraw);

        if(!needNotNull && !heightString.contains("\n"))
            return null;

        return GFontMetrics.getStringHeight(font, heightString);
    }

    protected String getDefaultWidthString(GPropertyDraw propertyDraw) {
        int defaultCharWidth = propertyDraw.charWidth != 0 ? propertyDraw.charWidth : getDefaultCharWidth();
        return GwtSharedUtils.replicate('0', defaultCharWidth);
    }

    protected String getDefaultHeightString(GPropertyDraw propertyDraw) {
        int defaultCharHeight = propertyDraw.charHeight != 0 ? propertyDraw.charHeight : getDefaultCharHeight();
        return "0" + GwtSharedUtils.replicate("\n0", defaultCharHeight - 1);
    }

    protected int getDefaultCharWidth() {
        throw new UnsupportedOperationException();
    }

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

    public GType getFilterMatchType() {
        return this;
    }
}
