package lsfusion.gwt.client.form.property.cell.classes.view.link;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ImageGridCellRenderer;

import static lsfusion.gwt.client.form.property.cell.classes.view.FileGridCellRenderer.ICON_EMPTY;

public class ImageLinkGridCellRenderer extends ImageGridCellRenderer {
    public ImageLinkGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected void setFileSrc(ImageElement img, Object value) {
        if (value instanceof String) {
            img.setSrc((String) value);
        } else {
            img.setSrc(GWT.getModuleBaseURL() + ICON_EMPTY);
        }
    }
}