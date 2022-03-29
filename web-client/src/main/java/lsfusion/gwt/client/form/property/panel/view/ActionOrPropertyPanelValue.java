package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.view.MainFrame;

import java.util.function.Consumer;

import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;

public class ActionOrPropertyPanelValue extends ActionOrPropertyValue implements ExecuteEditContext {

    public ActionOrPropertyPanelValue(GPropertyDraw property, GGroupObjectValue columnKey, GFormController form, boolean globalCaptionIsDrawn, ActionOrPropertyValueController controller) {
        super(property, columnKey, form, globalCaptionIsDrawn, controller);

        finalizeInit();
    }

    @Override
    protected void onAttach() {
        super.onAttach();

        // here and not in constructor, because tabIndex is set by default to 0 (see super)
        // in theory all renderer components should also be not focusable, otherwise during "tabbing" onFocus will return focus back, which will break tabbing
        if(!isFocusable()) // need to avoid selecting by tab
            setTabIndex(-1);
    }

    public boolean isFocusable() {
        return property.isFocusable();
    }

    private boolean readOnly;
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    protected void onEditEvent(EventHandler handler) {
        onEditEvent(handler, false);
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public boolean isPropertyReadOnly() {
        return isReadOnly();
    }

    @Override
    public void trySetFocus() {
        setFocus(true); // we can check if it's focusable, but it will be done automatically in onFocus
    }

    public void onBinding(Event event) {
        addStyleName("panelRendererValueBinding");
        Timer t = new Timer() {
            @Override
            public void run() {
                removeStyleName("panelRendererValueBinding");
                cancel();
            }
        };
        t.schedule(400);

        onEditEvent(new EventHandler(event), true);
    }
    public void onEditEvent(EventHandler handler, boolean isBinding) {
        form.executePropertyEventAction(handler, isBinding, this);
    }

    private boolean forceSetFocus;
    @Override
    public Object forceSetFocus() {
        forceSetFocus = true;
        return 0;
    }

    @Override
    public boolean isSetLastBlurred() {
        return true;
    }

    @Override
    public void restoreSetFocus(Object forceSetFocus) {
        this.forceSetFocus = false;
    }

    @Override
    protected void onFocus(EventHandler handler) {
        // prevent focusing
        if (!isFocusable() && !forceSetFocus && MainFrame.focusLastBlurredElement(handler, getElement())) {
            return;
        }

        super.onFocus(handler);
    }

    @Override
    public void pasteValue(String stringValue) {
        form.pasteValue(this, stringValue);
    }

    public void setBackground(String color) {
        GFormController.setBackgroundColor(getRenderElement(), getDisplayColor(color));
    }

    public void setForeground(String color) {
        GFormController.setForegroundColor(getRenderElement(), getDisplayColor(color));
    }

    @Override
    public Consumer<Object> getCustomRendererValueChangeConsumer() {
        return value -> form.changeProperty(this, value);
    }
}
