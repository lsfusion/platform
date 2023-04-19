package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.FileBasedCellRenderer;

public class FileCellRenderer extends FileBasedCellRenderer {
    public FileCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected BaseImage getBaseImage(PValue value) {
        return StaticImage.FILE;
    }
}
