package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.TooltipManager;

public abstract class GCountQuantityButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private final NumberFormat format;

    public GCountQuantityButton() {
        super("quantity.png", messages.formQueriesNumberOfEntries());
        format = NumberFormat.getDecimalFormat();
    }

    public void showPopup(int result, int clientX, int clientY) {
        TooltipManager.TooltipHelper tooltipHelper = new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return messages.formQueriesNumberOfEntries() + ": " + format.format(result);
            }

            @Override
            public boolean stillShowTooltip() {
                return isAttached() && isVisible();
            }
        };

        TooltipManager tooltipManager = TooltipManager.get();
        tooltipManager.hideTooltip(tooltipHelper);
        tooltipManager.showTooltip(clientX, clientY, tooltipHelper, true);
    }
}