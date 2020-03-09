package lsfusion.gwt.client.form.property.cell.classes.view.link;

import com.google.gwt.core.client.GWT;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ImageGridCellRenderer;

public class ImageLinkGridCellRenderer extends ImageGridCellRenderer {
    public ImageLinkGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String getFilePath(Object value) {
        return value instanceof String ? (String) value : GWT.getModuleBaseURL() + ICON_EMPTY;
    }
}