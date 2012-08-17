package platform.gwt.form2.shared.view.grid.renderer;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.i18n.client.NumberFormat;

public class NumberGridRenderer extends CellAdapterGridRenderer<Number> {
    public NumberGridRenderer() {
        this(NumberFormat.getDecimalFormat());
    }

    public NumberGridRenderer(NumberFormat format) {
        super(new NumberCell(format));
    }
}
