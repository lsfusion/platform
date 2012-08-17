package platform.gwt.form2.shared.view.grid.renderer;

import com.google.gwt.cell.client.TextCell;

public class StringGridRenderer extends CellAdapterGridRenderer<String> {
    public StringGridRenderer() {
        super(new TextCell());
    }
}
