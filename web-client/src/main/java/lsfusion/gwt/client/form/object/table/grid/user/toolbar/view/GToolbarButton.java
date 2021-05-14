package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.base.view.UnFocusableImageButton;

import static lsfusion.gwt.client.view.StyleDefaults.COMPONENT_HEIGHT_STRING;

public abstract class GToolbarButton extends UnFocusableImageButton {
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
        
        setTitle(tooltipText);
        
        addClickHandler(getClickHandler());
    }

    public abstract ClickHandler getClickHandler();

    public void showBackground(boolean showBackground) {
        getElement().getStyle().setBackgroundColor(showBackground ? "var(--selection-color)" : "");
        getElement().getStyle().setProperty("border", showBackground ? "1px solid var(--component-border-color)" : "");
    }
}
