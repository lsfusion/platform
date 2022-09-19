package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedCellRenderer;

public class FileCellRenderer extends FileBasedCellRenderer {
    public FileCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected Element createImage(Object value) {
        return (value == null ? StaticImage.EMPTY : StaticImage.FILE).createImage();
    }
}
