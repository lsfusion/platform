package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

// state view with tippy as a popup
public abstract class GTippySimpleStateTableView extends GSimpleStateTableView<Element> {

    private JavaScriptObject initTippy;

    public GTippySimpleStateTableView(GFormController form, GGridController grid, TableContainer tableContainer) {
        super(form, grid, tableContainer);

//        if(grid.recordView != null)
//            initTippy = GwtClientUtils.initTippyPopup(new PopupOwner(getPopupOwnerWidget()), getPopupElement(), "manual", null, null, () -> popupElementClicked);
    }

    private Element popupElementClicked;
    @Override
    protected JavaScriptObject showPopup(Element popupElement, Element popupElementClicked) {
//        this.popupElementClicked = popupElementClicked;
//        GwtClientUtils.showTippy(initTippy);
//        return initTippy;

        // the problem that in the container can be props rerender view, and thus popupElementClicked is deleted, but popup stays, so autoHidePartner refers to the deleted element which leads to the unpredictable behaviour
//        return GwtClientUtils.showTippyPopup(new PopupOwner(getPopupOwnerWidget(), popupElementClicked), popupElement, null);
        return GwtClientUtils.showTippyPopup(new PopupOwner(getPopupOwnerWidget()), popupElement, null, () -> popupElementClicked);
    }

    @Override
    protected void hidePopup(JavaScriptObject popup) {
//        assert initTippy == popup;
//        GwtClientUtils.hideTippy(popup, false, false);
//        popupElementClicked = null;

        GwtClientUtils.hideAndDestroyTippyPopup(popup);
    }
}
