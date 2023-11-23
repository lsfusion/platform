package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;

public abstract class GCountQuantityButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private final NumberFormat format;

    public GCountQuantityButton() {
        super(StaticImage.QUANTITY, messages.formQueriesNumberOfEntries());
        format = NumberFormat.getDecimalFormat();
    }

    JavaScriptObject popup;
    public void showPopup(int result) {
        popup = GwtClientUtils.showTippyPopup(getElement(),  new HTML(messages.formQueriesNumberOfEntries() + ": " + format.format(result)));
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        GwtClientUtils.hideTippyPopup(popup);
    }
}