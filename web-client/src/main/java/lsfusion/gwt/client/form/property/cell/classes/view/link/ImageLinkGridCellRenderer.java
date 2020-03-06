package lsfusion.gwt.client.form.property.cell.classes.view.link;

import com.google.gwt.core.client.GWT;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ImageGridCellRenderer;

import java.util.function.Consumer;

public class ImageLinkGridCellRenderer extends ImageGridCellRenderer {
    public ImageLinkGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected void setFilePath(Object value, Consumer<String> modifier) {
        modifier.accept(value instanceof String ? value.toString() : GWT.getModuleBaseURL() + ICON_EMPTY);
    }
}