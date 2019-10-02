package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import lsfusion.gwt.client.base.view.ImageButton;

public abstract class GToolbarButton extends ImageButton {
    public GToolbarButton(String imagePath) {
        this(imagePath, "");
    }

    public GToolbarButton(String imagePath, String tooltipText) {
        super(null, imagePath);

        addStyleName("toolbarButton");
        setTitle(tooltipText);
        addListener();
        setFocusable(false);
    }

    public abstract void addListener();

    public void showBackground(boolean showBackground) {
        getElement().getStyle().setBackgroundColor(showBackground ? "rgba(4, 137, 186, 0.09411764705882353)" : "");
        getElement().getStyle().setProperty("border", showBackground ? "1px solid #CCCCCC" : "");
    }
}
