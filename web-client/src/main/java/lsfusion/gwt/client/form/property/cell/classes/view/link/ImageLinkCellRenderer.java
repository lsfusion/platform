package lsfusion.gwt.client.form.property.cell.classes.view.link;

import lsfusion.gwt.client.base.AppLinkImage;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.classes.data.link.GLinkType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.view.ImageCellRenderer;

public class ImageLinkCellRenderer extends ImageCellRenderer {
    public ImageLinkCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected BaseImage getBaseImage(PValue value) {
        return new AppLinkImage(PValue.getStringValue(value), ((GLinkType) property.getValueType()).getExtension());
    }
}