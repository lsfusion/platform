package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
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
    public void start(EventHandler handler, Element parent, PValue oldValue) {
        PopupCellEditor.super.start(handler, parent, oldValue);
        Element tippyParent = GwtClientUtils.getTippyParent(parent);
        int left = tippyParent != null ? (tippyParent.getAbsoluteLeft() - parent.getAbsoluteLeft()) : parent.getAbsoluteLeft();
        int bottom = tippyParent != null ? (tippyParent.getAbsoluteBottom() - parent.getAbsoluteBottom()) : parent.getAbsoluteBottom();
        GwtClientUtils.showPopupInWindow(getPopup(), createPopupComponent(parent), left, bottom);
    }

    protected abstract Widget createPopupComponent(Element parent);

    public PopupValueCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager);
        this.property = property;
    }
}
