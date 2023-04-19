package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.form.property.GPropertyDraw;

public class TextCellRenderer extends StringBasedCellRenderer {

    public TextCellRenderer(GPropertyDraw property) {
        super(property, true);
    }

    @Override
    protected boolean isMultiLine() {
        return true;
    }
}