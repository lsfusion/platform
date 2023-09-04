package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.HTML;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.PopupDialogPanel;

public abstract class GCountQuantityButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private final NumberFormat format;

    public GCountQuantityButton() {
        super(StaticImage.QUANTITY, messages.formQueriesNumberOfEntries());
        format = NumberFormat.getDecimalFormat();
    }

    public void showPopup(int result, int clientX, int clientY) {
        GwtClientUtils.showPopupInWindow(new PopupDialogPanel(),
                new HTML(messages.formQueriesNumberOfEntries() + ": " + format.format(result)).asWidget(), clientX, clientY);
    }
}