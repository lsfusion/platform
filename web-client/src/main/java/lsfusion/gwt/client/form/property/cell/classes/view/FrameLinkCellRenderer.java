package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.base.AppLinkImage;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

public class FrameLinkCellRenderer extends FrameCellRenderer {

    public FrameLinkCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected BaseImage getBaseImage(PValue value) {
        return new AppLinkImage(PValue.getStringValue(value));
    }
}
