package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.base.view.ResizableHorizontalPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.view.StyleDefaults;

import java.math.BigDecimal;

public abstract class GCalculateSumButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public GCalculateSumButton() {
        super("sum.png", messages.formQueriesCalculateSum());
    }

    public void showPopup(Number result, GPropertyDraw property) {
        PopupDialogPanel popup = new PopupDialogPanel();
        popup.addStyleName("popup");

        ResizableHorizontalPanel panel = new ResizableHorizontalPanel();
        String caption = property.getNotEmptyCaption();
        Label text = new Label(result == null
                ? messages.formQueriesUnableToCalculateSum() + " [" + caption + "]"
                : messages.formQueriesSumResult() + " [" + caption + "]: ");
        panel.add(text);
        panel.add(GwtClientUtils.createHorizontalStrut(2));

        if (result != null) {
            TextBox valueBox = new TextBox();
            valueBox.addStyleName("popup-sumBox");
            valueBox.setHeight(StyleDefaults.VALUE_HEIGHT_STRING);
            panel.add(valueBox);
            NumberFormat format = NumberFormat.getDecimalFormat();
            if(result instanceof BigDecimal)
                format.overrideFractionDigits(0, ((BigDecimal)result).scale());
            valueBox.setValue(format.format(result));
            panel.setCellVerticalAlignment(text, HasAlignment.ALIGN_MIDDLE);
        }

        GwtClientUtils.showPopupInWindow(popup, new FocusPanel(panel), getAbsoluteLeft() + getOffsetWidth(), getAbsoluteTop());
    }
}