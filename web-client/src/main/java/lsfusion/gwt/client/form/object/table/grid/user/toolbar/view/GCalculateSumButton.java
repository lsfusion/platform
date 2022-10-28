package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.math.BigDecimal;

public abstract class GCalculateSumButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public GCalculateSumButton() {
        super(StaticImage.SUM, messages.formQueriesCalculateSum());
    }

    public void showPopup(Number result, GPropertyDraw property, int clientX, int clientY) {
        TooltipManager.TooltipHelper tooltipHelper = new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
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
                return text;
            }

            @Override
            public boolean stillShowTooltip() {
                return isAttached() && isVisible();
            }

            @Override
            public boolean stillShowSettingsButton() {
                return false;
            }
        };

        TooltipManager tooltipManager = TooltipManager.get();
        tooltipManager.showTooltip(clientX, clientY, tooltipHelper, true);
    }
}