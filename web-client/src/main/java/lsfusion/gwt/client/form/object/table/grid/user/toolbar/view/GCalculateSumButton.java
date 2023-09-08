package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.HTML;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.math.BigDecimal;

public abstract class GCalculateSumButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public GCalculateSumButton() {
        super(StaticImage.SUM, messages.formQueriesCalculateSum());
    }

    public void showPopup(Number result, GPropertyDraw property, int clientX, int clientY) {
        String caption = property.getNotEmptyCaption();
        String text = result == null
                ? messages.formQueriesUnableToCalculateSum() + " [" + caption + "]"
                : messages.formQueriesSumResult() + " [" + caption + "]: ";

        if (result != null) {
            NumberFormat format = NumberFormat.getDecimalFormat();
            if (result instanceof BigDecimal)
                format.overrideFractionDigits(0, ((BigDecimal) result).scale());
            text = text + format.format(result);
        }

        GwtClientUtils.showPopupInWindow(new PopupDialogPanel(), new HTML(text).asWidget(), clientX, clientY);
    }
}