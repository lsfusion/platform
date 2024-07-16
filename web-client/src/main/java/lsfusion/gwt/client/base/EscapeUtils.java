package lsfusion.gwt.client.base;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.DivWidget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.StaticImageWidget;

public class EscapeUtils {
    public static final String UNICODE_NBSP = "\u00A0";
    public static final String UNICODE_BULLET = "\u2022";

    // MESSAGE, CONFIRM, ASK
    public static Widget toHTML(String plainString, StaticImage image) {
        if(image != null) {
            FlexPanel iconMessagePanel = new FlexPanel();
            iconMessagePanel.add(getImageWidget(image));
            iconMessagePanel.add(toHTML(plainString.replace("<br/>", "<br />")), GFlexAlignment.CENTER, 1, true, null);
            return iconMessagePanel;
        } else {
            return toHTML(plainString);
        }
    }

    // toPrintMessage, tooltip
    public static DivWidget toHTML(String plainString) {
        DivWidget widget = new DivWidget();
        Element element = widget.getElement();
        GwtClientUtils.initCaptionHtmlOrText(element, CaptionHtmlOrTextType.MESSAGE); // maybe should be treated as Data
        GwtClientUtils.setCaptionHtmlOrText(element, plainString);
        return widget;
    }

    public static StaticImageWidget getImageWidget(StaticImage image) {
        StaticImageWidget imageWidget = new StaticImageWidget(image);
        imageWidget.addStyleName("right-padding fs-3");
        return imageWidget;
    }
}
