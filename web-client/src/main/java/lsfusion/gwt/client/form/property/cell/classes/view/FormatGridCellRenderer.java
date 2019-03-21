package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class FormatGridCellRenderer<T, F> extends TextBasedGridCellRenderer<T> {
    protected F format;

    public FormatGridCellRenderer(GPropertyDraw property) {
        super(property, Style.TextAlign.RIGHT);

        updateFormat();
    }

    public void updateFormat() {
        this.format = (F) property.getFormat();
    }

}
