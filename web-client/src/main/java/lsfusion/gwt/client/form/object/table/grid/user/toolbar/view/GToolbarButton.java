package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import lsfusion.gwt.client.base.view.ImageButton;

import static lsfusion.gwt.client.view.StyleDefaults.COMPONENT_HEIGHT_STRING;

public abstract class GToolbarButton extends ImageButton {
    public GToolbarButton(String imagePath) {
        this(imagePath, "");
    }

    public GToolbarButton(String caption, String imagePath, String tooltipText) {
        super(caption, imagePath);
        
        setSize(COMPONENT_HEIGHT_STRING, COMPONENT_HEIGHT_STRING);

        addStyleName("toolbarButton");
        setTitle(tooltipText);
        addListener();
    }
    public GToolbarButton(String imagePath, String tooltipText) {
        this(null, imagePath, tooltipText);
    }

    public abstract void addListener();

    public void showBackground(boolean showBackground) {
        getElement().getStyle().setBackgroundColor(showBackground ? "var(--selection-color)" : "");
        getElement().getStyle().setProperty("border", showBackground ? "1px solid var(--component-border-color)" : "");
    }
}
