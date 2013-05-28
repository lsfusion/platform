package platform.gwt.form.client.form.ui.toolbar;

import platform.gwt.form.shared.view.panel.ImageButton;

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
