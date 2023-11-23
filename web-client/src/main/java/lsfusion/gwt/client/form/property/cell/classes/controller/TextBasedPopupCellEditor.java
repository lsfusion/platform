package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public abstract class TextBasedPopupCellEditor extends SimpleTextBasedCellEditor {

    //protected final PopupDialogPanel popup = new PopupDialogPanel();
    protected InputElement editBox;
    public TextBasedPopupCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    public void onBlur(Event event, Element parent) {
        //if (!popup.isShowing())
            super.onBlur(event, parent);
    }

    // it seems that it's needed only for editBox.click() and selectAll
    @Override
    protected void onInputReady(Element parent, PValue oldValue) {
        editBox = inputElement;

        //popup.addAutoHidePartner(editBox);
        showTippyPopup(RootPanel.get().getElement(), parent, createPopupComponent(parent, oldValue).getElement());
    }

    protected native JavaScriptObject showTippyPopup(Element appendToElement, Element popupElementClicked, Element popupElement)/*-{
        var popup = $wnd.tippy(popupElementClicked, {
            appendTo : appendToElement,
            content : popupElement,
            trigger : 'manual',
            interactive : true,
            allowHTML : true
        });
        //popup.show();
        return popup;
    }-*/;

    protected abstract Widget createPopupComponent(Element parent, PValue oldValue);

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        super.clearRender(cellParent, renderContext, cancel);
        //popup.hide();
    }
}
