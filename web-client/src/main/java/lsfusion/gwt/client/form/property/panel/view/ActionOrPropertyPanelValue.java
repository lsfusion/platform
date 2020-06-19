package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class ActionOrPropertyPanelValue extends ActionOrPropertyValue {

    private final GGroupObjectValue columnKey;

    public ActionOrPropertyPanelValue(GPropertyDraw property, GGroupObjectValue columnKey, GFormController form) {
        super(property, form);

        this.columnKey = columnKey;

        //        if (!property.focusable) {
//            valueTable.setTableFocusable(false);
//        }

        finalizeInit();
    }

    private boolean readOnly;
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    protected RenderContext getRenderContext() {
        return RenderContext.DEFAULT;
    }

    @Override
    protected void onEditEvent(Event event, Runnable consumed) {
        onEditEvent(event, false, consumed);
    }

    public void onEditEvent(Event event, boolean forceChange, Runnable consumed) {
        form.executePropertyEventAction(property, columnKey, getRenderElement(), event, forceChange,
                this::getValue,
                this::setValue,
                () -> readOnly,
                getRenderContext(),
                getUpdateContext(),
                down -> {}, consumed);
    }

    @Override
    protected void onPaste(String objValue) {
        form.pasteSingleValue(property, columnKey, objValue);
    }
}
