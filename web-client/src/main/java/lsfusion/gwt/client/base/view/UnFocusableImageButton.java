package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.view.MainFrame;

public class UnFocusableImageButton extends ImageButton {

    public UnFocusableImageButton() {
        this(null, null);
    }

    public UnFocusableImageButton(String caption, String imagePath) {
        super(caption, imagePath);

        sinkEvents(Event.ONFOCUS);
        setFocusable(false);
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (BrowserEvents.FOCUS.equals(event.getType()) && MainFrame.focusLastBlurredElement(new EventHandler(event), getElement())) {
            return;
        }

        super.onBrowserEvent(event);
    }
}
