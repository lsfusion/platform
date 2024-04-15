package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.BaseStaticImage;
import lsfusion.gwt.client.base.FocusUtils;

public class UnFocusableImageButton extends StaticImageButton {

    public UnFocusableImageButton(String caption, BaseStaticImage image) {
        super(caption, image);

        sinkEvents(Event.ONFOCUS);
        setFocusable(false);
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (BrowserEvents.FOCUS.equals(event.getType()) && !ignoreFocusLastBlurredElement() && FocusUtils.focusLastBlurredElement(new EventHandler(event), getElement())) {
            return;
        }

        super.onBrowserEvent(event);
    }

    protected boolean ignoreFocusLastBlurredElement() {
        return false;
    }
}
