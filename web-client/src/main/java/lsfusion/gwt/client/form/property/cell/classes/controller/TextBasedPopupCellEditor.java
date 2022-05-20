package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public abstract class TextBasedPopupCellEditor extends TextBasedCellEditor {

    protected final PopupDialogPanel popup = new PopupDialogPanel();
    protected InputElement editBox;
    public TextBasedPopupCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    public void onBlur(Event event, Element parent) {
        if (!popup.isShowing())
            super.onBlur(event, parent);
    }

    @Override
    public void start(Event event, Element parent, Object oldValue) {
        editBox = getInputElement(parent);

        GwtClientUtils.showPopupInWindow(popup, createPopupComponent(parent, oldValue), parent.getAbsoluteLeft(), parent.getAbsoluteBottom());
        popup.addAutoHidePartner(editBox);
        super.start(event, parent, oldValue);
    }

    protected abstract Widget createPopupComponent(Element parent, Object oldValue);

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        super.clearRender(cellParent, renderContext, cancel);
        popup.hide();
    }
}
