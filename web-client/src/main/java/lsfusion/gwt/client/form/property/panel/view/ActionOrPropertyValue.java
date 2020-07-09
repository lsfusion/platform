package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.text.ParseException;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;

// property value renderer with editing
public abstract class ActionOrPropertyValue extends FocusWidget implements EditContext {

    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    protected GPropertyDraw property;

    protected GFormController form;

    public ActionOrPropertyValue(GPropertyDraw property, GFormController form) {
        setElement(Document.get().createDivElement());

        DataGrid.initSinkEvents(this);

        this.property = property;

        this.form = form;

        getRenderElement().setPropertyObject("groupObject", property.groupObject);

        // aligning values vertically
        GPropertyTableBuilder.setLineHeight(getRenderElement(), getHeight());
    }

    public Element getRenderElement() {
        return getElement();
    }

    @Override
    public Element getFocusElement() {
        return getElement();
    }

    protected void finalizeInit() {
        this.form.render(this.property, getRenderElement(), getRenderContext());
    }

    private Widget borderWidget;

    // when we don't want property value (it's content) to influence on layouting, and in particular flex - basis
    // so we use absolute positioning for that (and not width 100%, or writing to div itself)
    public void setStatic(ResizableSimplePanel panel, boolean isProperty) {
        panel.setFillWidget(this);
        borderWidget = panel;

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

        FlexPanel.setBaseWidth(borderWidget, getWidth(), getHeight());
    }

    public int getHeight() {
        return property.getValueHeight(null);
    }

    public int getWidth() {
        return property.getValueWidth(null);
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element target = DataGrid.getTargetAndCheck(getElement(), event);
        if(target == null)
            return;
        if(!form.previewClickEvent(target, event))
            return;

        super.onBrowserEvent(event);

        if(!DataGrid.checkSinkEvents(event))
            return;

        EventHandler handler = createEventHandler(event);

        if(BrowserEvents.FOCUS.equals(event.getType())) {
            onFocus(handler);
        } else if(BrowserEvents.BLUR.equals(event.getType())) {
            onBlur(handler);
        }
        if(handler.consumed)
            return;

        form.onPropertyBrowserEvent(handler, getRenderElement(), getFocusElement(),
                () -> {}, // no outer context
                () -> onEditEvent(handler),
                () -> {}, // no outer context
                () -> CopyPasteUtils.putIntoClipboard(getRenderElement()), () -> CopyPasteUtils.getFromClipboard(handler, line -> pasteValue(line)));
    }

    protected void onFocus(EventHandler handler) {
        borderWidget.addStyleName("panelRendererValueFocused");
    }

    protected void onBlur(EventHandler handler) {
        form.previewBlurEvent(handler.event);

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

    public RenderContext getRenderContext() {
        return new RenderContext() {};
    }

    public UpdateContext getUpdateContext() {
        return UpdateContext.DEFAULT;
    }

    protected abstract void onPaste(String objValue);

    public void pasteValue(final String value) {
        Scheduler.get().scheduleDeferred(() -> {
            Object objValue = null;
            try {
                objValue = property.baseType.parseString(value, property.pattern);
            } catch (ParseException ignored) {}
            updateValue(objValue);

            onPaste(value);
        });
    }

    public void updateValue(Object value) {
        setValue(value);

        form.update(property, getRenderElement(), getValue(), getUpdateContext());
    }

    public void setBackground(String color) {
        GFormController.setBackgroundColor(getRenderElement(), getDisplayColor(color));
    }

    public void setForeground(String color) {
        GFormController.setForegroundColor(getRenderElement(), getDisplayColor(color));
    }
}
