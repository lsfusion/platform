package lsfusion.client.form.property.cell.classes.view;

import lsfusion.base.file.AppFileDataImage;
import lsfusion.client.form.property.ClientPropertyDraw;

import java.awt.*;

public class VideoPropertyRenderer extends ImagePropertyRenderer {
    public VideoPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);

        setIcon(value != null ? convertValue(((AppFileDataImage) value)) : null);
    }

    public static void expandImage(final AppFileDataImage value) {
        if (value != null) {
            expandImage(convertValue(value));
        }
    }

    public static Image convertValue(AppFileDataImage value) {
        //video is not supported
        return null;
    }
}
