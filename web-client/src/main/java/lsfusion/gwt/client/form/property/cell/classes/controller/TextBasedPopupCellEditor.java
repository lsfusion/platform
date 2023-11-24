package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public abstract class TextBasedPopupCellEditor extends SimpleTextBasedCellEditor {

    protected InputElement editBox;
    public TextBasedPopupCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    // it seems that it's needed only for editBox.click() and selectAll
    @Override
    protected void onInputReady(Element parent, PValue oldValue) {
        editBox = inputElement;

        GwtClientUtils.showTippyPopup(RootPanel.get().getElement(), parent, createPopupComponent(parent, oldValue).getElement(), false);
    }

    protected abstract Widget createPopupComponent(Element parent, PValue oldValue);
}
