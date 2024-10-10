package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GEventSource;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import static lsfusion.gwt.client.view.MainFrame.v5;

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
        form.executePropertyEventAction(handler, this);
    }

    @Override
    public Boolean isReadOnly() {
        return this.isPropertyReadOnly();
    }

    @Override
    public Boolean isPropertyReadOnly() {
        Boolean readonly = property.isReadOnly();
        if(readonly != null)
            return readonly;
        return super.isPropertyReadOnly();
    }

    @Override
    public CellRenderer.ToolbarAction[] getToolbarActions() {
        return !property.toolbarActions || this.isPropertyReadOnly() != null ? super.getToolbarActions() : property.getQuickAccessActions(true, isFocused);
    }

    public void onBinding(Event event) {
        GwtClientUtils.addClassName(this, "panel-renderer-value-binding", "panelRendererValueBinding", v5);
        Timer t = new Timer() {
            @Override
            public void run() {
                GwtClientUtils.removeClassName(ActionOrPropertyPanelValue.this, "panel-renderer-value-binding", "panelRendererValueBinding", v5);
                cancel();
            }
        };
        t.schedule(400);

        form.onPropertyBinding(event, this);
    }

    @Override
    public void setLoading() {
        this.loading = true;

        controller.setLoading(columnKey, PValue.getPValue(true));
    }

    @Override
    public void startEditing() {
        super.startEditing();
        
        controller.startEditing(columnKey);
    }

    @Override
    public void stopEditing() {
        super.stopEditing();

        controller.stopEditing(columnKey);
    }

    public PValue setLoadingValue(PValue value) {
        PValue oldValue = getValue();

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
    protected void onFocus(Element target, EventHandler handler) {
        // prevent focusing
        if (!isFocusable() && !forceSetFocus && FocusUtils.focusLastBlurredElement(handler, getElement())) {
            return;
        }

        super.onFocus(target, handler);
    }

    @Override
    public void pasteValue(String stringValue) {
        form.pasteValue(this, stringValue);
    }

    @Override
    public void getAsyncValues(String value, String actionSID, AsyncCallback<GFormController.GAsyncResult> callback, int increaseValuesNeededCount) {
        form.getAsyncValues(value, this, actionSID, callback, increaseValuesNeededCount);
    }

    @Override
    public void changeProperty(PValue result, GFormController.ChangedRenderValueSupplier renderValueSupplier) {
        form.changeProperty(this, result, GEventSource.CUSTOM, renderValueSupplier);
    }

    @Override
    public RendererType getRendererType() {
        return RendererType.PANEL;
    }

    @Override
    protected GComponent getComponent() {
        return property;
    }

    @Override
    public boolean isInputRemoveAllPMB() {
        return false;
    }
}
