package lsfusion.gwt.form.client.form.ui.toolbar;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.ui.ResizableHorizontalPanel;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

import java.math.BigDecimal;

public abstract class GCalculateSumButton extends GToolbarButton {

    public GCalculateSumButton() {
        super("sum.png", "Посчитать сумму");
    }

    public void showPopup(Number result, GPropertyDraw property) {
        PopupPanel popup = new PopupPanel(true, false);
        popup.addStyleName("popup");

        ResizableHorizontalPanel panel = new ResizableHorizontalPanel();
        Label text = new Label(result == null
                ? "Невозможно посчитать сумму [" + property.caption + "]"
                : "Сумма [" + property.caption + "]: ");
        panel.add(text);

        if (result != null) {
            TextBox valueBox = new TextBox();
            valueBox.addStyleName("popup-sumBox");
            panel.add(valueBox);
            NumberFormat format = NumberFormat.getDecimalFormat();
            if(result instanceof BigDecimal)
                format.overrideFractionDigits(0, ((BigDecimal)result).scale());
            valueBox.setValue(format.format(result));
            panel.setCellVerticalAlignment(text, HasAlignment.ALIGN_MIDDLE);
        }

        popup.setWidget(panel);
        popup.setPopupPosition(getAbsoluteLeft() + getOffsetWidth(), getAbsoluteTop());
        popup.show();
    }
}
