package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

// state view with tippy as a popup
public abstract class GTippySimpleStateTableView extends GSimpleStateTableView<Element> {

    public GTippySimpleStateTableView(GFormController form, GGridController grid, TableContainer tableContainer) {
        super(form, grid, tableContainer);
    }

    @Override
    protected JavaScriptObject showPopup(Widget ownerWidget, Element popupElement, Element popupElementClicked) {
        return GwtClientUtils.showTippyPopup(ownerWidget, popupElementClicked, popupElement, null);
    }

    @Override
    protected void hidePopup(JavaScriptObject popup) {
        GwtClientUtils.hideTippyPopup(popup);
    }
}
