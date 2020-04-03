package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableHorizontalPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.math.BigDecimal;

public abstract class GCalculateSumButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public GCalculateSumButton() {
        super("sum.png", messages.formQueriesCalculateSum());
    }

    public void showPopup(Number result, GPropertyDraw property) {
        PopupPanel popup = new PopupPanel(true, false);
        popup.addStyleName("popup");

        ResizableHorizontalPanel panel = new ResizableHorizontalPanel();
        Label text = new Label(result == null
                ? messages.formQueriesUnableToCalculateSum() + " [" + property.caption + "]"
                : messages.formQueriesSumResult() + " [" + property.caption + "]: ");
        panel.add(text);
        panel.add(GwtClientUtils.createHorizontalStrut(2));

        if (result != null) {
            TextBox valueBox = new TextBox();
            valueBox.addStyleName("popup-sumBox");
            valueBox.setHeight(GwtClientUtils.VALUE_HEIGHT_STRING);
            panel.add(valueBox);
            NumberFormat format = NumberFormat.getDecimalFormat();
            if(result instanceof BigDecimal)
                format.overrideFractionDigits(0, ((BigDecimal)result).scale());
            valueBox.setValue(format.format(result));
            panel.setCellVerticalAlignment(text, HasAlignment.ALIGN_MIDDLE);
        }

        popup.setWidget(panel);
        GwtClientUtils.showPopupInWindow(popup, getAbsoluteLeft() + getOffsetWidth(), getAbsoluteTop());
    }
}
