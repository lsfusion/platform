package lsfusion.gwt.client.base;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.DivWidget;

public class EscapeUtils {
    public static final String UNICODE_NBSP = "\u00A0";
    public static final String UNICODE_BULLET = "\u2022";

    // MESSAGE, CONFIRM, ASK, tooltip
    public static Widget toHTML(String plainString) {
        DivWidget widget = new DivWidget();
        GwtClientUtils.setCaptionHtmlOrText(widget.getElement(), plainString);
        return widget;
    }
}
