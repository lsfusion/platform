package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public abstract class TextBasedPopupCellEditor extends TextBasedCellEditor {

    protected final PopupDialogPanel popup = new PopupDialogPanel();
    protected InputElement editBox;
    public TextBasedPopupCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    public void commit(Element parent, CommitReason commitReason) {
        if (popup.isShowing() && commitReason.isBlurred()) {
            // popup will automatically close on date selection, on closing it will change the value of the editElement and return focus to it,
            //the editElement will lose focus and this method will be called but the popup will not be visible and the super.commit will be triggered
        } else {
            super.commit(parent, commitReason);
        }
    }

    @Override
    public void start(Event event, Element parent, Object oldValue) {
        super.start(event, parent, oldValue);
        editBox = getInputElement(parent);

        GwtClientUtils.showPopupInWindow(popup, createPopupComponent(parent, oldValue), parent.getAbsoluteLeft(), parent.getAbsoluteBottom());
        popup.addAutoHidePartner(editBox);
    }

    protected abstract Widget createPopupComponent(Element parent, Object oldValue);

    protected void setInputValue(Object value) {
        setInputValue(editBox, value);
    }

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        popup.hide();
    }
}
