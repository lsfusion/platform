package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class FormatGridCellRenderer<T, F> extends TextBasedGridCellRenderer<T> {
    protected F format;

    public FormatGridCellRenderer(GPropertyDraw property) {
        super(property);

        updateFormat();
    }

    public void updateFormat() {
        this.format = (F) property.getFormat();
    }

}
