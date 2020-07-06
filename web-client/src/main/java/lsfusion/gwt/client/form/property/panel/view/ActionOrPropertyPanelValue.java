package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GInputEvent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class ActionOrPropertyPanelValue extends ActionOrPropertyValue implements ExecuteEditContext {

    private final GGroupObjectValue columnKey;

    public ActionOrPropertyPanelValue(GPropertyDraw property, GGroupObjectValue columnKey, GFormController form) {
        super(property, form);

        this.columnKey = columnKey;

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
        if(property.focusable != null)
            return property.focusable;
        return !property.hasKeyBinding();
    }

    private boolean readOnly;
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    protected void onEditEvent(EventHandler handler) {
        onEditEvent(handler, null);
    }

    @Override
    public GGroupObjectValue getColumnKey() {
        return columnKey;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void trySetFocus() {
        setFocus(true); // we can check if it's focusable, but it will be done automatically in onFocus
    }

    public void onBinding(GInputEvent bindingEvent, Event event) {
        onEditEvent(new EventHandler(event), bindingEvent);
    }
    public void onEditEvent(EventHandler handler, GInputEvent bindingEvent) {
        form.executePropertyEventAction(handler, bindingEvent, this);
    }

    private boolean forceSetFocus;
    @Override
    public Object forceSetFocus() {
        forceSetFocus = true;
        return 0;
    }

    @Override
    public void restoreSetFocus(Object forceSetFocus) {
        this.forceSetFocus = false;
    }

    @Override
    protected void onFocus(EventHandler handler) {
        if(!isFocusable() && !forceSetFocus) { // prevent focusing
            Element lastBlurredElement = form.getLastBlurredElement();
            // in theory we also have to check if focused element still visible, isShowing in GwtClientUtils but now it's assumed that it is always visible
            if(lastBlurredElement != null && lastBlurredElement != getElement()) { // return focus back where it was
                handler.consume();
                lastBlurredElement.focus();
                return;
            }
        }

        super.onFocus(handler);
    }

    @Override
    protected void onPaste(String objValue) {
        form.pasteSingleValue(property, columnKey, objValue);
    }
}
