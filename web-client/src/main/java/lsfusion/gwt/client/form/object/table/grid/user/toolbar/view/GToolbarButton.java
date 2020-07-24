package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.view.StyleDefaults.COMPONENT_HEIGHT_STRING;

public abstract class GToolbarButton extends ImageButton {
    public GToolbarButton(String imagePath) {
        this(imagePath, "");
    }

    public GToolbarButton(String imagePath, String tooltipText) {
        this(null, imagePath, tooltipText, true);
    }

    public GToolbarButton(String caption, String imagePath, String tooltipText, boolean compact) {
        super(caption, imagePath);

        addStyleName("toolbarButton");
        if (compact) {
            setSize(COMPONENT_HEIGHT_STRING, COMPONENT_HEIGHT_STRING);
            addStyleName("toolbarButtonNoBorder");
        }
        
        sinkEvents(Event.ONFOCUS);
        
        setTitle(tooltipText);
        
        addClickHandler(getClickHandler());
        
        setFocusable(false);
    }

    public abstract ClickHandler getClickHandler();

    public void showBackground(boolean showBackground) {
        getElement().getStyle().setBackgroundColor(showBackground ? "var(--selection-color)" : "");
        getElement().getStyle().setProperty("border", showBackground ? "1px solid var(--component-border-color)" : "");
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (BrowserEvents.FOCUS.equals(event.getType()) && MainFrame.focusLastBlurredElement(new EventHandler(event), getElement())) {
            return;
        }

        super.onBrowserEvent(event);
    }
}
