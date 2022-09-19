package lsfusion.gwt.client.form.property.cell.classes.view.link;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ImageCellRenderer;

public class ImageLinkCellRenderer extends ImageCellRenderer {
    public ImageLinkCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected Element createImage(Object value) {
        if(value instanceof String)
            return GwtClientUtils.createImage((String) value);
        else
            return StaticImage.EMPTY.createImage();
    }
}