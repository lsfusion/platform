package lsfusion.gwt.client.form.property.panel.view;

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
    protected void onEditEvent(EventHandler handler) {
        onEditEvent(handler, false);
    }

    public void onEditEvent(EventHandler handler, boolean forceChange) {
        form.executePropertyEventAction(property, columnKey, getRenderElement(), handler, forceChange,
                this::getValue,
                this::setValue,
                () -> readOnly,
                getRenderContext(),
                getUpdateContext(),
                down -> {});
    }

    @Override
    protected void onPaste(String objValue) {
        form.pasteSingleValue(property, columnKey, objValue);
    }
}
