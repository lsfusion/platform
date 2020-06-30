package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class ActionOrPropertyPanelValue extends ActionOrPropertyValue {

    private final GGroupObjectValue columnKey;

    public ActionOrPropertyPanelValue(GPropertyDraw property, GGroupObjectValue columnKey, GFormController form) {
        super(property, form);

        this.columnKey = columnKey;

        if(!isFocusable()) // need to avoid selecting by tab
            setTabIndex(-1);

        finalizeInit();
    }

    private boolean isFocusable() {
        if(property.focusable != null)
            return property.focusable;
        return property.changeKey == null;
    }

    private boolean readOnly;
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    protected void onEditEvent(EventHandler handler) {
        onEditEvent(handler, false);
    }

    public void onEditEvent(EventHandler handler, boolean forceChange) {
        form.executePropertyEventAction(property, columnKey, getRenderElement(), handler, forceChange,
                this::getValue,
                this::setValue,
                () -> readOnly,
                getRenderContext(),
                getUpdateContext());
    }

    @Override
    protected void onFocus(EventHandler handler) {
        if(!isFocusable()) {
            Element lastBlurredElement = form.getLastBlurredElement();
            // in theory we also have to check if focused element still visible,
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
