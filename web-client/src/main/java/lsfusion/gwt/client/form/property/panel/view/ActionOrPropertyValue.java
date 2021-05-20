package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static lsfusion.gwt.client.base.GwtClientUtils.setupFillParent;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;

// property value renderer with editing
public abstract class ActionOrPropertyValue extends FocusWidget implements EditContext, RenderContext, UpdateContext {

    private Object value;

    public Object getValue() {
        return value;
    }

    // editing set value (in EditContext), changes model and value itself
    public void setValue(Object value) {
        this.value = value; // updating inner model

        controller.setValue(columnKey, value); // updating outer model - controller
    }

    protected GPropertyDraw property;
    protected GGroupObjectValue columnKey;

    protected GFormController form;
    protected ActionOrPropertyValueController controller;

    public ActionOrPropertyValue(GPropertyDraw property, GGroupObjectValue columnKey, GFormController form, ActionOrPropertyValueController controller) {
        setElement(Document.get().createDivElement());

        DataGrid.initSinkEvents(this);

        this.property = property;
        this.columnKey = columnKey;

        this.form = form;
        this.controller = controller;

        getRenderElement().setPropertyObject("groupObject", property.groupObject);
    }

    public Element getRenderElement() {
        return getElement();
    }

    @Override
    public Element getFocusElement() {
        return getElement();
    }

    protected void finalizeInit() {
        this.form.render(this.property, getRenderElement(), this);
    }

    private Widget borderWidget;

    // when we don't want property value (it's content) to influence on layouting, and in particular flex - basis
    // so we use absolute positioning for that (and not width 100%, or writing to div itself)
    public void setStatic(ResizableMainPanel panel, boolean isProperty) { // assert that panel is resizable, panel and not resizable simple panel, since we want to append corners also to that panel (and it is not needed for it to be simple)
        panel.setMain(this);
        setupFillParent(getElement());
        borderWidget = panel.getPanelWidget();

        setBaseSize(isProperty);
    }
    public void setDynamic(ResizableMainPanel panel, boolean isProperty) {
        panel.setMain(this);
        com.google.gwt.dom.client.Element element = getElement();
        element.getStyle().setWidth(100, Style.Unit.PCT);
        element.getStyle().setHeight(100, Style.Unit.PCT);
        borderWidget = panel.getPanelWidget();

        setBaseSize(isProperty);
    }
    public void setDynamic(boolean isProperty) {
        borderWidget = this;

        setBaseSize(isProperty);
    }

    public void setBaseSize(boolean isProperty) {
        // we have to set border for border element and not element itself, since absolute positioning include border INSIDE div, and default behaviour is OUTSIDE
        borderWidget.addStyleName("panelRendererValue");
        if(isProperty)
            borderWidget.addStyleName("propertyPanelRendererValue");
        else
            borderWidget.addStyleName("actionPanelRendererValue");

        // if widget is wrapped into absolute positioned simple panel, we need to include paddings (since borderWidget doesn't include them)
        boolean isStatic = borderWidget != this;
        FlexPanel.setBaseSize(borderWidget,
                isStatic ? property.getValueWidthWithPadding(null) : property.getValueWidth(null),
                isStatic ? property.getValueHeightWithPadding(null ) : property.getValueHeight(null));
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element target = DataGrid.getTargetAndCheck(getElement(), event);
        if(target == null)
            return;
        if(!form.previewEvent(target, event))
            return;

        super.onBrowserEvent(event);

        if(!DataGrid.checkSinkEvents(event))
            return;

        EventHandler eventHandler = createEventHandler(event);

        if(BrowserEvents.FOCUS.equals(event.getType())) {
            onFocus(eventHandler);
        } else if(BrowserEvents.BLUR.equals(event.getType())) {
            onBlur(eventHandler);
        }
        if(eventHandler.consumed)
            return;

        form.onPropertyBrowserEvent(eventHandler, getRenderElement(), getFocusElement(),
                handler -> {}, // no outer context
                this::onEditEvent,
                handler -> {}, // no outer context
                //ctrl-c ctrl-v from excel adds \n in the end, trim() removes it
                handler -> CopyPasteUtils.putIntoClipboard(getRenderElement()), handler -> CopyPasteUtils.getFromClipboard(handler, line -> pasteValue(line.trim())), true);
    }

    protected void onFocus(EventHandler handler) {
        DataGrid.sinkPasteEvent(getFocusElement());
        borderWidget.addStyleName("panelRendererValueFocused");
    }

    protected void onBlur(EventHandler handler) {
        borderWidget.removeStyleName("panelRendererValueFocused");
    }

    public EventHandler createEventHandler(Event event) {
        return new EventHandler(event);
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

    public RenderContext getRenderContext() {
        return this;
    }

    @Override
    public Integer getStaticHeight() {
        return null;
    }

    @Override
    public boolean isAlwaysSelected() {
        return true;
    }

    @Override
    public boolean globalCaptionIsDrawn() {
        return false;
    }

    @Override
    public GFont getFont() {
        return null;
    }

    @Override
    public boolean isStaticHeight() {
        return false;
    }

    public UpdateContext getUpdateContext() {
        return this;
    }

    public abstract void pasteValue(final String value);

    public void updateValue(Object value) {
        this.value = value;

        form.update(property, getRenderElement(), value, this);
    }
}
