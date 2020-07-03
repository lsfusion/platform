package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

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

    public void addFill(FlexPanel panel) {
        panel.add(this, panel.getWidgetCount(), GFlexAlignment.STRETCH, 1, property.getValueWidth(null), property.getValueHeight(null));
    }

    public void setBaseSize() {
        FlexPanel.setBaseWidth(this, property.getValueWidth(null), property.getValueHeight(null));
//        setWidth(property.getValueWidth(null) + "px");
//        setHeight(property.getValueHeight(null) + "px");
    }

    @Override
    public void onBrowserEvent(Event event) {
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

        form.onPropertyBrowserEvent(handler, getRenderElement(),
            () -> onEditEvent(handler),
            () -> CopyPasteUtils.putIntoClipboard(getRenderElement()),
            () -> CopyPasteUtils.getFromClipboard(handler, line -> pasteValue(line))
        );
    }

    protected void onFocus(EventHandler handler) {
        addStyleName("dataPanelRendererGridPanelFocused");
    }

    protected void onBlur(EventHandler handler) {
        form.previewBlurEvent(handler.event);

        removeStyleName("dataPanelRendererGridPanelFocused");
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
