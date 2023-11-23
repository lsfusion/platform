package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.math.BigDecimal;

public abstract class GCalculateSumButton extends GToolbarButton {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public GCalculateSumButton() {
        super(StaticImage.SUM, messages.formQueriesCalculateSum());
    }

    JavaScriptObject popup;
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

        //GwtClientUtils.showPopupInWindow(new PopupDialogPanel(), new HTML(text).asWidget(), clientX, clientY);
        popup = showTippyPopup(RootPanel.get().getElement(), getElement(), new HTML(text).getElement());
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