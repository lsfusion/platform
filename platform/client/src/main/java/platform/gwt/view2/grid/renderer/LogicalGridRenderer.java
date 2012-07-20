package platform.gwt.view2.grid.renderer;

import com.google.gwt.cell.client.CheckboxCell;

public class LogicalGridRenderer extends CellAdapterGridRenderer<Boolean> {
    public LogicalGridRenderer() {
        super(new CheckboxCell());
    }
}
