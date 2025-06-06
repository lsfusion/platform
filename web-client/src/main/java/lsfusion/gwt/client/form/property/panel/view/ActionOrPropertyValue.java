package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.view.MainFrame.v5;

// property value renderer with editing
public abstract class ActionOrPropertyValue extends Widget implements EditContext, RenderContext, UpdateContext, ColorThemeChangeListener {

    protected PValue value;
    protected boolean loading;
    private AppBaseImage image;
    private String valueElementClass;
    private GFont font;
    private String background;
    private Object foreground;
    protected Boolean readOnly;
    private String placeholder;
    private String pattern;
    private String regexp;
    private String regexpMessage;
    private String valueTooltip;
    private PValue propertyCustomOption;
    private GInputBindingEvent changeKey;
    private GInputBindingEvent changeMouse;

    public PValue getValue() {
        return value;
    }

    // editing set value (in EditContext), changes model and value itself
    public void setValue(PValue value) {
        this.value = value; // updating inner model

        controller.setValue(columnKey, value); // updating outer model - controller
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public boolean isSelectedRow() {
        return true;
    }

    @Override
    public AppBaseImage getImage() {
        return image;
    }

    @Override
    public Boolean isPropertyReadOnly() {
        if(readOnly != null && !readOnly && property.isAction() && MainFrame.disableActionsIfReadonly)
            return true;
        return readOnly;
    }

    @Override
    public boolean isTabFocusable() {
        return isFocusable();
    }

    @Override
    public boolean isNavigateInput() {
        return true;
    }

    @Override
    public GFont getFont() {
        return font;
    }

    @Override
    public String getBackground() {
        return background;
    }

    @Override
    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public String getRegexp() {
        return regexp;
    }

    @Override
    public String getRegexpMessage() {
        return regexpMessage;
    }

    @Override
    public String getValueTooltip() {
        return valueTooltip;
    }

    @Override
    public PValue getPropertyCustomOptions() {
        return propertyCustomOption;
    }

    @Override
    public String getForeground() {
        return foreground != null ? foreground.toString() : null;
    }

    @Override
    public String getValueElementClass() {
        return valueElementClass;
    }

    protected GPropertyDraw property;
    protected GGroupObjectValue columnKey;

    protected GFormController form;
    protected ActionOrPropertyValueController controller;

    private boolean globalCaptionIsDrawn;

    public ActionOrPropertyValue(GPropertyDraw property, GGroupObjectValue columnKey, GFormController form, boolean globalCaptionIsDrawn, ActionOrPropertyValueController controller) {
        this.property = property;
        this.columnKey = columnKey;

        this.form = form;
        this.controller = controller;

        this.globalCaptionIsDrawn = globalCaptionIsDrawn;

        MainFrame.addColorThemeChangeListener(this);
    }

    public Element getRenderElement() {
        return getElement();
    }

    @Override
    public Element getEditElement() {
        return getRenderElement();
    }

    @Override
    public Element getFocusElement() {
        Element element = getRenderElement();

        Object focusElement = CellRenderer.getFocusElement(element);
        if(focusElement != null) {
            return focusElement == CellRenderer.NULL ? null : (Element) focusElement;
        }

        return element;
    }

    protected void render() {
        Element renderElement = property.getCellRenderer(getRendererType()).createRenderElement(getRendererType());
        this.form.render(this.property, renderElement, this);
        setElement(renderElement);

        DataGrid.initSinkEvents(this);
        DataGrid.initSinkFocusEvents(this);

        GFormController.setBindingGroupObject(this,  property.groupObject);

        Element focusElement = getFocusElement();
        if(focusElement != null)
            focusElement.setTabIndex(isFocusable() ? 0 : -1);

       GwtClientUtils.addClassName(this, "panel-renderer-value", "panelRendererValue", v5);
    }

    public void focus(FocusUtils.Reason reason) {
        Element focusElement = getFocusElement();
        if(focusElement != null)
            FocusUtils.focus(focusElement, reason);
    }

    public SizedWidget getSizedWidget(boolean needNotNull) {
        boolean globalCaptionIsDrawn = this.globalCaptionIsDrawn;
        GFont font = getFont();
        GSize valueWidth = property.getValueWidth(font, needNotNull, globalCaptionIsDrawn);
        GSize valueHeight = property.getValueHeight(font, needNotNull, globalCaptionIsDrawn);

        Element renderElement = getRenderElement();
        Element sizeElement = InputBasedCellRenderer.getSizeElement(renderElement);
        GwtClientUtils.addClassName(sizeElement, "prop-size-value");

        if(sizeElement != renderElement) {
            FlexPanel.setPanelWidth(sizeElement, valueWidth);
            FlexPanel.setPanelHeight(sizeElement, valueHeight);

            valueWidth = null;
            valueHeight = null;
        }

        return new SizedWidget(this, valueWidth, valueHeight);
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element target = form.getTargetAndPreview(getElement(), event);
        if(target == null)
            return;

        super.onBrowserEvent(event);

        EventHandler eventHandler = new EventHandler(event);
        DataGrid.dispatchFocusAndCheckSinkEvents(eventHandler, target, getElement(), this::onFocus, this::onBlur);
        if(eventHandler.consumed)
            return;

        form.onPropertyBrowserEvent(eventHandler, getRenderElement(), true, getFocusElement(), // we don't need to focus unfocusable element (because the focus will be returned immediately)
                handler -> {}, // no outer context
                this::onEditEvent,
                handler -> {}, // no outer context
                //ctrl-c ctrl-v from excel adds \n in the end, trim() removes it
                handler -> CopyPasteUtils.putIntoClipboard(getRenderElement()), handler -> CopyPasteUtils.getFromClipboard(handler, line -> pasteValue(line.trim())),
                true, property.getCellRenderer(getRendererType()).isCustomRenderer(), isFocusable());
    }

    protected boolean isFocused;
    protected void onFocus(Element target, EventHandler handler) {
        if(isFocused)
            return;

        Element renderElement = getRenderElement();
        Object focusElement = CellRenderer.getFocusElement(renderElement);
        if(focusElement == null || focusElement == CellRenderer.NULL)
            DataGrid.sinkPasteEvent(renderElement);

        isFocused = true;
        focusedChanged();
    }

    private void focusedChanged() {
        form.checkFocusElement(isFocused, getRenderElement());

        if(isFocused)
           GwtClientUtils.addClassName(this, "panel-renderer-value-focused", "panelRendererValueFocused", v5);
        else
           GwtClientUtils.removeClassName(this, "panel-renderer-value-focused", "panelRendererValueFocused", v5);
        update();
    }

    protected void onBlur(Element target, EventHandler handler) {
        if(!isFocused)
            return;
        //if !isFocused should be replaced to assert; isFocused must be true, but sometimes is not (related to BusyDialogDisplayer)
        //assert isFocused;
        isFocused = false;
        focusedChanged();
    }

    public boolean isEditing;
    @Override
    public void startEditing() {
        isEditing = true;
        GwtClientUtils.addClassName(this, "panel-renderer-value-edited", "panelRendererValueEdited", v5);
    }

    @Override
    public void stopEditing() {
        isEditing = false;
        GwtClientUtils.removeClassName(this, "panel-renderer-value-edited", "panelRendererValueEdited", v5);
    }

    protected abstract void onEditEvent(EventHandler handler);

    @Override
    public GPropertyDraw getProperty() {
        return property;
    }

    @Override
    public GGroupObjectValue getColumnKey() {
        return columnKey;
    }

    @Override
    public GGroupObjectValue getRowKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Widget getPopupOwnerWidget() {
        return this;
    }

    public RenderContext getRenderContext() {
        return this;
    }

    @Override
    public boolean globalCaptionIsDrawn() {
        return globalCaptionIsDrawn;
    }

    public UpdateContext getUpdateContext() {
        return this;
    }

    public abstract void pasteValue(final String value);

    public void update(PValue value, boolean loading, AppBaseImage image, String valueElementClass,
                       GFont font, String background, String foreground, Boolean readOnly, String placeholder, String pattern,
                       String regexp, String regexpMessage, String valueTooltip, PValue propertyCustomOption) {
        this.value = value;
        this.loading = loading;
        this.image = image;
        this.valueElementClass = valueElementClass;
        this.font = font;
        this.background = background;
        this.foreground = foreground;
        this.readOnly = readOnly;
        this.placeholder = placeholder;
        this.pattern = pattern;
        this.regexp = regexp;
        this.regexpMessage = regexpMessage;
        this.valueTooltip = valueTooltip;
        this.propertyCustomOption = propertyCustomOption;

        update();
    }

    @Override
    public void colorThemeChanged() {
        update();
    }

    @Override
    public GFormController getForm() {
        return form;
    }

    protected void update() {
        // RERENDER IF NEEDED : we have the previous state

        form.update(property, getRenderElement(), this);
    }

    protected abstract GComponent getComponent();
}
