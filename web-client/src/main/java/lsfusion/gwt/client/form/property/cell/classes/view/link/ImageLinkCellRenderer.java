package lsfusion.gwt.client.form.property.cell.classes.view.link;

import lsfusion.gwt.client.base.AppLinkImage;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ImageCellRenderer;

public class ImageLinkCellRenderer extends ImageCellRenderer {
    public ImageLinkCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected BaseImage getBaseImage(Object value) {
        return new AppLinkImage((String) value);
    }
}