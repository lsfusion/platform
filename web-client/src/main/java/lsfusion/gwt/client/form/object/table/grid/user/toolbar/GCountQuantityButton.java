package lsfusion.gwt.client.form.object.table.grid.user.toolbar;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import lsfusion.gwt.client.ClientMessages;

public abstract class GCountQuantityButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private NumberFormat format;

    public GCountQuantityButton() {
        super("quantity.png", messages.formQueriesNumberOfEntries());
        format = NumberFormat.getDecimalFormat();
    }

    public void showPopup(int result) {
        PopupPanel popup = new PopupPanel(true, false);
        popup.addStyleName("popup");
        popup.setWidget(new Label(messages.formQueriesNumberOfEntries() + ": " + format.format(result)));
        popup.setPopupPosition(getAbsoluteLeft() + getOffsetWidth(), getAbsoluteTop());
        popup.show();
    }
}
