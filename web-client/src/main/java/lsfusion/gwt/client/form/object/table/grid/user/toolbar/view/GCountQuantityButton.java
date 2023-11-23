package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
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

    JavaScriptObject popup;
    public void showPopup(int result, int clientX, int clientY) {
        //GwtClientUtils.showPopupInWindow(new PopupDialogPanel(),
        //        new HTML(messages.formQueriesNumberOfEntries() + ": " + format.format(result)).asWidget(), clientX, clientY);
        popup = showTippyPopup(RootPanel.get().getElement(), getElement(),  new HTML(messages.formQueriesNumberOfEntries() + ": " + format.format(result)).getElement());
    }

    protected native JavaScriptObject showTippyPopup(Element appendToElement, Element popupElementClicked, Element popupElement)/*-{
        var popup = $wnd.tippy(popupElementClicked, {
            appendTo : appendToElement,
            content : popupElement,
            trigger : 'manual',
            interactive : true,
            allowHTML : true
        });
        popup.show();
        return popup;
    }-*/;


    @Override
    protected void onDetach() {
        super.onDetach();
        if(popup != null) {
            hideTippyPopup(popup);
        }
    }

    protected native void hideTippyPopup(JavaScriptObject popup)/*-{
        // probably it should be checked if popup's already hidden, but it seems, that there is no such method
        popup.hide();
        popup.destroy();
    }-*/;
}