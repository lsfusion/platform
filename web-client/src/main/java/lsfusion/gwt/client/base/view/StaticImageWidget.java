package lsfusion.gwt.client.base.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.StaticImage;

public class StaticImageWidget extends Widget {
    public StaticImageWidget(StaticImage image) {
        setElement(image.createImage());
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return addHandler(handler, ClickEvent.getType());
    }
}
