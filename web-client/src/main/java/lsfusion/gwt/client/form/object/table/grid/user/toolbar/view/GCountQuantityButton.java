package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Label;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PopupDialogPanel;

public abstract class GCountQuantityButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private NumberFormat format;

    public GCountQuantityButton() {
        super("quantity.png", messages.formQueriesNumberOfEntries());
        format = NumberFormat.getDecimalFormat();
    }

    public void showPopup(int result) {
        PopupDialogPanel popup = new PopupDialogPanel();
        popup.addStyleName("popup");
        GwtClientUtils.showPopupInWindow(popup, new FocusPanel(new Label(messages.formQueriesNumberOfEntries() + ": " + format.format(result))), getAbsoluteLeft() + getOffsetWidth(), getAbsoluteTop());
    }
}