package lsfusion.gwt.form.client.form.ui.toolbar;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.ui.ResizableHorizontalPanel;
import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

public abstract class GCalculateSumButton extends GToolbarButton {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();
    private NumberFormat format;

    public GCalculateSumButton() {
        super("sum.png", messages.formQueriesCalculateSum());
        format = NumberFormat.getDecimalFormat();
    }

    public void showPopup(Number result, GPropertyDraw property) {
        PopupPanel popup = new PopupPanel(true, false);
        popup.addStyleName("popup");

        ResizableHorizontalPanel panel = new ResizableHorizontalPanel();
        Label text = new Label(result == null
                ? messages.formQueriesUnableToCalculateSum() + " [" + property.caption + "]"
                : messages.formQueriesSumResult() + " [" + property.caption + "]: ");
        panel.add(text);

        if (result != null) {
            TextBox valueBox = new TextBox();
            valueBox.addStyleName("popup-sumBox");
            panel.add(valueBox);
            valueBox.setValue(format.format(result));
            panel.setCellVerticalAlignment(text, HasAlignment.ALIGN_MIDDLE);
        }

        popup.setWidget(panel);
        popup.setPopupPosition(getAbsoluteLeft() + getOffsetWidth(), getAbsoluteTop());
        popup.show();
    }
}
