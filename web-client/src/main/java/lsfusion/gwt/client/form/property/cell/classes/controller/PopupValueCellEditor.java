package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.WindowValueCellEditor;

public abstract class PopupValueCellEditor extends WindowValueCellEditor implements PopupCellEditor {
    protected final SafeHtmlRenderer<String> renderer = SimpleSafeHtmlRenderer.getInstance();

    protected GPropertyDraw property;

    protected PopupDialogPanel popup;

    @Override
    public void setPopup(PopupDialogPanel popup) {
        this.popup = popup;
    }

    @Override
    public PopupDialogPanel getPopup() {
        return popup;
    }

    @Override
    public void start(EventHandler handler, Element parent, Object oldValue) {
        PopupCellEditor.super.start(handler, parent, oldValue);
        GwtClientUtils.showPopupInWindow(getPopup(), createPopupComponent(parent, oldValue), parent.getAbsoluteLeft(), parent.getAbsoluteBottom());
    }

    protected abstract Widget createPopupComponent(Element parent, Object oldValue);

    public PopupValueCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager);
        this.property = property;
    }
}
