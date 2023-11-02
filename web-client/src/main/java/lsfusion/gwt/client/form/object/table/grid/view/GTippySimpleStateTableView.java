package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

// state view with tippy as a popup
public abstract class GTippySimpleStateTableView extends GSimpleStateTableView<Element> {

    public GTippySimpleStateTableView(GFormController form, GGridController grid, TableContainer tableContainer) {
        super(form, grid, tableContainer);
    }

    @Override
    public void onBrowserEvent(Event event) {
        if(GMouseStroke.isDownEvent(event)) {
            EventTarget target = event.getEventTarget();
            Element popupElement = getPopupElement(popupObject);
            if(popupElement != null) {
                if(Element.is(target) && !popupElement.isOrHasChild(Element.as(target))) {
                    hidePopup();
                }
            }
        }
        super.onBrowserEvent(event);
    }

    private native Element getPopupElement(JavaScriptObject popupObject)/*-{
        return popupObject != null ? popupObject.popper : null;
    }-*/;


    @Override
    protected JavaScriptObject showPopup(Element popupElementClicked, Element popupElement) {
        return showTippyPopup(popupElementClicked, popupElement);
    }

    @Override
    protected void hidePopup(JavaScriptObject popup) {
        hideTippyPopup(popup);
    }

    protected native JavaScriptObject showTippyPopup(Element popupElementClicked, Element popupElement)/*-{
        var popup = $wnd.tippy(popupElementClicked, {
            content : popupElement,
            trigger : 'manual',
            interactive : true,
            allowHTML : true,
            hideOnClick: false
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
