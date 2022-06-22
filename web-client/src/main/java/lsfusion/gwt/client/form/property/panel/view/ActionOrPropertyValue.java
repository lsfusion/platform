package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.view.ColorUtils.getThemedColor;

// property value renderer with editing
public abstract class ActionOrPropertyValue extends FocusWidget implements EditContext, RenderContext, UpdateContext, ColorThemeChangeListener {

    protected Object value;
    protected boolean loading;
    private Object image;
    private Object background;
    private Object foreground;
    protected boolean readOnly;

    public Object getValue() {
        return value;
    }

    // editing set value (in EditContext), changes model and value itself
    public void setValue(Object value) {
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
    public Object getImage() {
        return image;
    }

    @Override
    public boolean isPropertyReadOnly() {
        return readOnly;
    }

    @Override
    public String getBackground(String baseColor) {
        return getThemedColor(background != null ? background.toString() : baseColor);
    }

    @Override
    public String getForeground() {
        return getThemedColor(foreground != null ? foreground.toString() : null);
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
    public Element getEditEventElement() {
        return getRenderElement();
    }

    @Override
    public Element getFocusElement() {
        return getElement();
    }

    protected void finalizeInit() {
        render();
    }

    private Widget borderWidget;

    // assert that panel is resizable, panel and not resizable simple panel, since we want to append corners also to that panel (and it is not needed for it to be simple)
    public SizedWidget setSized(ResizableMainPanel panel) {
        boolean autoSize = property.autoSize;
        if(panel != null) {
            panel.setSizedMain(this, autoSize);
            borderWidget = panel.getPanelWidget(); // panel
        } else {
//            assert autoSize;
            borderWidget = this;
        }

        setBorderStyles();

        GSize width;
        GSize height;
        if(!autoSize) {
//            assert panel != null;
            width = property.getValueWidth(null);
            height = property.getValueHeight(null);
        } else {
            width = property.getAutoSizeValueWidth(null);
            height = property.getAutoSizeValueHeight(null);

//            if(panel != null) { // sort of optimization, in this case paddings will be calculated automatically
//                FlexPanel.setBaseSize(this, false, width, false);
//                FlexPanel.setBaseSize(this, true, height, false);
//                return new SizedWidget(borderWidget);
//            }
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

    protected boolean isFocused;
    protected void onFocus(EventHandler handler) {
        if(isFocused)
            return;
        DataGrid.sinkPasteEvent(getFocusElement());

        isFocused = true;
        borderWidget.addStyleName("panelRendererValueFocused");
        update();
    }

    protected void onBlur(EventHandler handler) {
        if(!isFocused || DataGrid.isFakeBlur(handler.event, getElement())) {
            return;
        }
        //if !isFocused should be replaced to assert; isFocused must be true, but sometimes is not (related to LoadingManager)
        //assert isFocused;
        isFocused = false;
        borderWidget.removeStyleName("panelRendererValueFocused");
        update();
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

    private void render() {
        this.form.render(this.property, getRenderElement(), this);
    }

    public void update(Object value, boolean loading, Object image, Object background, Object foreground, boolean readOnly) {
        this.value = value;
        this.loading = loading;
        this.image = image;
        this.background = background;
        this.foreground = foreground;
        this.readOnly = readOnly;

        update();
    }

    @Override
    public void colorThemeChanged() {
        update();
    }

    protected void update() {
        // RERENDER IF NEEDED : we have the previous state

        form.update(property, getRenderElement(), this);
    }
}
