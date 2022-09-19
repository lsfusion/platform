package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.view.MainFrame;

public class ActionOrPropertyPanelValue extends ActionOrPropertyValue implements ExecuteEditContext {

    public ActionOrPropertyPanelValue(GPropertyDraw property, GGroupObjectValue columnKey, GFormController form, boolean globalCaptionIsDrawn, ActionOrPropertyValueController controller) {
        super(property, columnKey, form, globalCaptionIsDrawn, controller);

        render();
    }

    public boolean isFocusable() {
        return property.isFocusable();
    }

    @Override
    protected void onEditEvent(EventHandler handler) {
        onEditEvent(handler, false);
    }

    @Override
    public boolean isReadOnly() {
        return isPropertyReadOnly();
    }

    @Override
    public boolean isPropertyReadOnly() {
        return super.isPropertyReadOnly() || property.isReadOnly();
    }

    @Override
    public CellRenderer.ToolbarAction[] getToolbarActions() {
        return isPropertyReadOnly() ? super.getToolbarActions() : property.getQuickAccessActions(true, isFocused);
    }

    @Override
    public void trySetFocusOnBinding() {
        focus(FocusUtils.Reason.BINDING); // we can check if it's focusable, but it will be done automatically in onFocus
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

    @Override
    public void setLoading() {
        this.loading = true;

        controller.setLoading(columnKey, true);
    }

    public Object setLoadingValue(Object value) {
        Object oldValue = getValue();

        setLoading();
        setValue(value);

        update();

        return oldValue;
    }

    private boolean forceLoading;

    public void setForceLoading(boolean forceLoading) {
        this.forceLoading = forceLoading;

        update();
    }

    @Override
    public boolean isLoading() {
        return super.isLoading() || forceLoading;
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

    @Override
    public void changeProperty(Object result) {
        form.changeProperty(this, result);
    }

    @Override
    public void executeContextAction(int action) {
        form.executeContextAction( this, action);
    }
}
