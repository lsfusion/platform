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

    // toPrintMessage, tooltip
    public static Widget toHTML(String plainString) {
        return toHTML(plainString, null);
    }

    // MESSAGE, CONFIRM, ASK
    public static Widget toHTML(String plainString, StaticImage image) {
        if(image != null) {
            FlexPanel panel = new FlexPanel();

            StaticImageWidget imageWidget = new StaticImageWidget(image);
            imageWidget.addStyleName("fs-3");
            imageWidget.addStyleName("right-padding");
            panel.add(imageWidget);

            panel.add(getPlainStringWidget(plainString), GFlexAlignment.CENTER);

            return panel;
        } else {
            return getPlainStringWidget(plainString);
        }

    }

    private static DivWidget getPlainStringWidget(String plainString) {
        DivWidget widget = new DivWidget();
        Element element = widget.getElement();
        GwtClientUtils.initCaptionHtmlOrText(element, CaptionHtmlOrTextType.MESSAGE); // maybe should be treated as Data
        GwtClientUtils.setCaptionHtmlOrText(element, plainString);
        return widget;
    }
}
