package lsfusion.gwt.form.client.grid.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

import static lsfusion.gwt.form.client.grid.renderer.FileGridCellRenderer.ICON_EMPTY;

public class ImageLinkGridCellRenderer extends ImageGridCellRenderer {
    public ImageLinkGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected void setImageSrc(ImageElement img, Object value) {
        if (value instanceof String) {
            img.setSrc((String) value);
        } else {
            img.setSrc(GWT.getModuleBaseURL() + "images/" + ICON_EMPTY);
        }
    }
}