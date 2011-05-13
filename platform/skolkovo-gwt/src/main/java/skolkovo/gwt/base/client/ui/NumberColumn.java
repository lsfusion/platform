package skolkovo.gwt.base.client.ui;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.user.cellview.client.Column;

public abstract class NumberColumn<T> extends Column<T, Number> {
    public NumberColumn() {
        super(new NumberCell());
    }
}
