package lsfusion.gwt.client.form.object.table.grid.user.toolbar.view;

import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.base.BaseStaticImage;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.UnFocusableImageButton;

public abstract class GToolbarButton extends UnFocusableImageButton {
    public GToolbarButton(BaseStaticImage image) {
        this(image, "");
    }

    public GToolbarButton(BaseStaticImage image, String tooltipText) {
        this(null, image, tooltipText);
    }

    public GToolbarButton(String caption, BaseStaticImage image, String tooltipText) {
        super(caption, image);

        setTitle(tooltipText);
        
        addClickHandler(getClickHandler());
    }

    public abstract ClickHandler getClickHandler();

    public void showBackground(boolean showBackground) {
        if (showBackground)
            GwtClientUtils.addClassName(this, "active");
        else
            GwtClientUtils.removeClassName(this, "active");
    }
}
