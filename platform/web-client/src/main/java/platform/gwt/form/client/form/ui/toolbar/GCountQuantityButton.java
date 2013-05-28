package platform.gwt.form.client.form.ui.toolbar;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;

public abstract class GCountQuantityButton extends GToolbarButton {
    private NumberFormat format;

    public GCountQuantityButton() {
        super("quantity.png", "Количество записей");
        format = NumberFormat.getDecimalFormat();
    }

    public void showPopup(int result) {
        PopupPanel popup = new PopupPanel(true, false);
        popup.addStyleName("popup");
        popup.setWidget(new Label("Количество записей: " + format.format(result)));
        popup.setPopupPosition(getAbsoluteLeft() + getOffsetWidth(), getAbsoluteTop());
        popup.show();
    }
}
