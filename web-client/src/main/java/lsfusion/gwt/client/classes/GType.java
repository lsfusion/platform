package lsfusion.gwt.client.classes;

import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;
import lsfusion.gwt.client.form.property.panel.view.PropertyPanelRenderer;

import java.io.Serializable;
import java.text.ParseException;

public abstract class GType implements Serializable {
    public PanelRenderer createPanelRenderer(GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, Result<CaptionWidget> captionContainer) {
        return new PropertyPanelRenderer(form, controller, property, columnKey, captionContainer);
    }

    public abstract CellRenderer createCellRenderer(GPropertyDraw property);

    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return null;
    }

    public GSize getValueWidth(GFont font, GPropertyDraw propertyDraw, boolean needNotNull, boolean globalCaptionIsDrawn) {
        if(propertyDraw.charWidth != -1)
            return getDefaultCharWidth(font, propertyDraw);

        return getDefaultWidth(font, propertyDraw, globalCaptionIsDrawn);
    }

    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean globalCaptionIsDrawn) {
        return getDefaultCharWidth(font, propertyDraw);
    }

    private GSize getDefaultCharWidth(GFont font, GPropertyDraw propertyDraw) {
        return GFontMetrics.getStringWidth(font, propertyDraw.charWidth != -1 ? GFontMetrics.getDefaultWidthString(propertyDraw.charWidth) : getDefaultWidthString(propertyDraw));
    }

    public GSize getValueHeight(GFont font, GPropertyDraw propertyDraw, boolean globalCaptionIsDrawn) {
        if(propertyDraw.charHeight != -1)
            return getDefaultCharHeight(font, propertyDraw);

        return getDefaultHeight(font, propertyDraw, globalCaptionIsDrawn);
    }

    public GSize getDefaultHeight(GFont font, GPropertyDraw propertyDraw, boolean globalCaptionIsDrawn) {
        return getDefaultCharHeight(font, propertyDraw);
    }

    private GSize getDefaultCharHeight(GFont font, GPropertyDraw propertyDraw) {
        return GFontMetrics.getStringHeight(font, propertyDraw.charHeight != -1 ? propertyDraw.charHeight : getDefaultCharHeight());
    }

    protected String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return GFontMetrics.getDefaultWidthString(getDefaultCharWidth());
    }

    protected int getDefaultCharWidth() {
        throw new UnsupportedOperationException();
    }

    public int getDefaultCharHeight() {
        return 1;
    }

    public abstract GCompare[] getFilterCompares();
    public abstract PValue parseString(String s, String pattern) throws ParseException;

    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return null;
    }

    public boolean isId(){
        return false;
    }

    public GType getFilterMatchType() {
        return this;
    }

    private final static GInputType inputType = new GInputType("text");
    public GInputType getValueInputType() {
        return inputType;
    }
}
