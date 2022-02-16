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

    private boolean globalCaptionIsDrawn;

    public ActionOrPropertyValue(GPropertyDraw property, GGroupObjectValue columnKey, GFormController form, boolean globalCaptionIsDrawn, ActionOrPropertyValueController controller) {
        setElement(Document.get().createDivElement());

        DataGrid.initSinkEvents(this);
        DataGrid.initSinkFocusEvents(this);

        this.property = property;
        this.columnKey = columnKey;

        this.form = form;
        this.controller = controller;

        this.globalCaptionIsDrawn = globalCaptionIsDrawn;

        getRenderElement().setPropertyObject("groupObject", property.groupObject);
    }

    public Element getRenderElement() {
        return getElement();
    }

    @Override
    public Element getEditElement() {
        return getRenderElement();
    }

    @Override
    public Element getEditEventElement() {
        return getRenderElement();
    }

    @Override
    public Element getFocusElement() {
        return getElement();
    }

    protected void finalizeInit() {
        this.form.render(this.property, getRenderElement(), this);
    }

    private Widget borderWidget;

    // assert that panel is resizable, panel and not resizable simple panel, since we want to append corners also to that panel (and it is not needed for it to be simple)
    public SizedWidget setSized(ResizableMainPanel panel) {
        boolean autoSize = property.autoSize;
        if(panel != null) {
            panel.setSizedMain(this, autoSize);
            borderWidget = panel.getPanelWidget(); // panel
        } else {
            assert autoSize;
            borderWidget = this;
        }

        setBorderStyles();

        Integer width;
        Integer height;
        if(!autoSize) {
            assert panel != null;
            width = property.getValueWidthWithPadding(null);
            height = property.getValueHeightWithPadding(null);
        } else {
            width = property.getAutoSizeValueWidth(null);
            height = property.getAutoSizeValueHeight(null);

            if(panel != null) { // sort of optimization, in this case paddings will be calculated automatically
                FlexPanel.setBaseSize(this, false, width, false);
                FlexPanel.setBaseSize(this, true, height, false);
                return new SizedWidget(borderWidget);
            }
        }

        return new SizedWidget(borderWidget, width, height);
    }

    private void setBorderStyles() {
        // we have to set border for border element and not element itself, since absolute positioning include border INSIDE div, and default behaviour is OUTSIDE
        borderWidget.addStyleName("panelRendererValue");
        if(property.boxed)
            borderWidget.addStyleName("panelRendererValueBoxed");
        if(property.isAction())
            borderWidget.addStyleName("actionPanelRendererValue");
        else
            borderWidget.addStyleName("propertyPanelRendererValue");
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element target = DataGrid.getTargetAndCheck(getElement(), event);
        if(target == null)
            return;
        if(!form.previewEvent(target, event))
            return;

        super.onBrowserEvent(event);

        if(!DataGrid.checkSinkEvents(event) && !DataGrid.checkSinkFocusEvents(event))
            return;

        EventHandler eventHandler = new EventHandler(event);

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
                handler -> CopyPasteUtils.putIntoClipboard(getRenderElement()), handler -> CopyPasteUtils.getFromClipboard(handler, line -> pasteValue(line.trim())),
                true, property.getCellRenderer().isCustomRenderer());

        form.propagateFocusEvent(event);
    }

    private boolean isFocused;
    protected void onFocus(EventHandler handler) {
        if(isFocused)
            return;
        DataGrid.sinkPasteEvent(getFocusElement());
        isFocused = true;

        borderWidget.addStyleName("panelRendererValueFocused");
    }

    protected void onBlur(EventHandler handler) {
        if(!isFocused || DataGrid.isFakeBlur(handler.event, getElement())) {
            return;
        }
        //if !isFocused should be replaced to assert; isFocused must be true, but sometimes is not (related to LoadingManager)
        //assert isFocused;
        isFocused = false;
        borderWidget.removeStyleName("panelRendererValueFocused");
    }

    public boolean isEditing;
    @Override
    public void startEditing() {
        isEditing = true;
        borderWidget.addStyleName("panelRendererValueEdited");
    }

    @Override
    public void stopEditing() {
        isEditing = false;
        borderWidget.removeStyleName("panelRendererValueEdited");
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

    public RenderContext getRenderContext() {
        return this;
    }

    @Override
    public boolean isAlwaysSelected() {
        return true;
    }

    @Override
    public boolean globalCaptionIsDrawn() {
        return globalCaptionIsDrawn;
    }

    @Override
    public GFont getFont() {
        return null;
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
