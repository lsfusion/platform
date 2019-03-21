package lsfusion.gwt.client.form.object.table.grid.user.toolbar;

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
}
