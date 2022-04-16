package lsfusion.gwt.client.form.property.cell.classes.view.link;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ImageCellRenderer;

public class ImageLinkCellRenderer extends ImageCellRenderer {
    public ImageLinkCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String getFilePath(Object value) {
        return value instanceof String ? (String) value : GwtClientUtils.getStaticImageURL(ICON_EMPTY);
    }
}