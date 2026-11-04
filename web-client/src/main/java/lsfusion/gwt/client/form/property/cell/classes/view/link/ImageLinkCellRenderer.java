package lsfusion.gwt.client.form.property.cell.classes.view.link;

import lsfusion.gwt.client.base.AppLinkImage;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.classes.data.link.GLinkType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.view.ImageCellRenderer;

public class ImageLinkCellRenderer extends ImageCellRenderer {
    private final GLinkType type; // not the property's: a footer of another type is drawn by a renderer of its own

    public ImageLinkCellRenderer(GPropertyDraw property, GLinkType type) {
        super(property);
        this.type = type;
    }

    @Override
    protected BaseImage getBaseImage(PValue value) {
        return new AppLinkImage(PValue.getStringValue(value), type.getExtension());
    }
}