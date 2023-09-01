package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.TooltipManager;

public abstract class GCountQuantityButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private final NumberFormat format;

    public GCountQuantityButton() {
        super(StaticImage.QUANTITY, messages.formQueriesNumberOfEntries());
        format = NumberFormat.getDecimalFormat();

        TooltipManager.initTooltip(getElement(), null);
    }

    public void showPopup(int result) {
        TooltipManager.TooltipHelper tooltipHelper = new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return messages.formQueriesNumberOfEntries() + ": " + format.format(result);
            }

            @Override
            public boolean stillShowSettingsButton() {
                return false;
            }
        };

        TooltipManager.show(getElement(), tooltipHelper);
    }
}