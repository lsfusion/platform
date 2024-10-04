package lsfusion.gwt.client.base.view;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;

public class StaticImageWidget extends Widget {
    public StaticImageWidget(StaticImage image) {
        setElement(image.createImage());
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONCLICK);

        addHandler(mouseOverEvent -> GwtClientUtils.addClassName(this, "cursor-pointer"), MouseOverEvent.getType());
        addHandler(mouseOutEvent -> GwtClientUtils.removeClassName(this, "cursor-pointer"), MouseOutEvent.getType());

        return addHandler(handler, ClickEvent.getType());
    }
}
