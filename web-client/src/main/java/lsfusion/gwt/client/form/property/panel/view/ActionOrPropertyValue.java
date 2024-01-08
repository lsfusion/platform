package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.view.ColorUtils.getThemedColor;

// property value renderer with editing
public abstract class ActionOrPropertyValue extends Widget implements EditContext, RenderContext, UpdateContext, ColorThemeChangeListener {

    protected PValue value;
    protected boolean loading;
    private AppBaseImage image;
    private String valueElementClass;
    private String background;
    private Object foreground;
    protected Boolean readOnly;
    private String placeholder;
    private String pattern;
    private String regexp;
    private String regexpMessage;
    private String valueTooltip;

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

        addStyleName("panelRendererValue");
    }

    public void focus(FocusUtils.Reason reason) {
        Element focusElement = getFocusElement();
        if(focusElement != null)
            FocusUtils.focus(focusElement, reason);
    }

    public SizedWidget getSizedWidget() {
        boolean globalCaptionIsDrawn = this.globalCaptionIsDrawn;
        GFont font = getFont();
        RendererType rendererType = getRendererType();
        GSize valueWidth = property.getValueWidth(font, false, globalCaptionIsDrawn, rendererType);
        GSize valueHeight = property.getValueHeight(font, false, globalCaptionIsDrawn, rendererType);

        Element renderElement = getRenderElement();
        Element sizeElement = InputBasedCellRenderer.getSizeElement(renderElement);
        sizeElement.addClassName("prop-size-value");
        if(!property.isShrinkOverflowVisible())
            sizeElement.addClassName("prop-value-shrink");

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
        Element target = DataGrid.getBrowserTargetAndCheck(getElement(), event);
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

        form.onPropertyBrowserEvent(eventHandler, getRenderElement(), true, getFocusElement(),
                handler -> {}, // no outer context
                this::onEditEvent,
                handler -> {}, // no outer context
                //ctrl-c ctrl-v from excel adds \n in the end, trim() removes it
                handler -> CopyPasteUtils.putIntoClipboard(getRenderElement()), handler -> CopyPasteUtils.getFromClipboard(handler, line -> pasteValue(line.trim())),
                true, property.getCellRenderer(getRendererType()).isCustomRenderer());

        form.propagateFocusEvent(event);
    }

    protected boolean isFocused;
    protected void onFocus(EventHandler handler) {
        if(isFocused)
            return;

        Element renderElement = getRenderElement();
        Object focusElement = CellRenderer.getFocusElement(renderElement);
        if(focusElement == null || focusElement == CellRenderer.NULL)
            DataGrid.sinkPasteEvent(renderElement);

        isFocused = true;
        addStyleName("panelRendererValueFocused");
        update();
    }

    protected void onBlur(EventHandler handler) {
        if(!isFocused || DataGrid.isFakeBlur(handler.event, getElement())) {
            return;
        }
        //if !isFocused should be replaced to assert; isFocused must be true, but sometimes is not (related to LoadingManager)
        //assert isFocused;
        isFocused = false;
        removeStyleName("panelRendererValueFocused");
        update();
    }

    public boolean isEditing;
    @Override
    public void startEditing() {
        isEditing = true;
        addStyleName("panelRendererValueEdited");
    }

    @Override
    public void stopEditing() {
        isEditing = false;
        removeStyleName("panelRendererValueEdited");
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

    public void update(PValue value, boolean loading, AppBaseImage image, String valueElementClass,
                       String background, String foreground, Boolean readOnly, String placeholder, String pattern,
                       String regexp, String regexpMessage, String valueTooltip) {
        this.value = value;
        this.loading = loading;
        this.image = image;
        this.valueElementClass = valueElementClass;
        this.background = background;
        this.foreground = foreground;
        this.readOnly = readOnly;
        this.placeholder = placeholder;
        this.pattern = pattern;
        this.regexp = regexp;
        this.regexpMessage = regexpMessage;
        this.valueTooltip = valueTooltip;

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
}
