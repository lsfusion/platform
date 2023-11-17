package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

// state view with tippy as a popup
public abstract class GTippySimpleStateTableView extends GSimpleStateTableView<Element> {

    public GTippySimpleStateTableView(GFormController form, GGridController grid, TableContainer tableContainer) {
        super(form, grid, tableContainer);
    }

    @Override
    protected JavaScriptObject showPopup(Element popupElementClicked, Element popupElement) {
        return showTippyPopup(RootPanel.get().getElement(), popupElementClicked, popupElement);
    }

    @Override
    protected void hidePopup(JavaScriptObject popup) {
        hideTippyPopup(popup);
    }

    protected native JavaScriptObject showTippyPopup(Element appendToElement, Element popupElementClicked, Element popupElement)/*-{
        var popup = $wnd.tippy(popupElementClicked, {
            appendTo : appendToElement,
            content : popupElement,
            trigger : 'manual',
            interactive : true,
            allowHTML : true
        });
        popup.show();
        return popup;
    }-*/;

    protected native void hideTippyPopup(JavaScriptObject popup)/*-{
        // probably it should be checked if popup's already hidden, but it seems, that there is no such method
        popup.hide();
        popup.destroy();
    }-*/;

}
