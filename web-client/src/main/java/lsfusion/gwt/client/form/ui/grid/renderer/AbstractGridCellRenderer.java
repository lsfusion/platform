package lsfusion.gwt.client.form.ui.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.client.form.ui.cellview.DataGrid;
import lsfusion.gwt.client.form.ui.cellview.cell.Cell;
import lsfusion.gwt.client.MainFrameMessages;

public abstract class AbstractGridCellRenderer implements GridCellRenderer {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();
    protected final String EMPTY_VALUE = messages.formRendererNotDefined();
    protected final String REQUIRED_VALUE = messages.formRendererRequired();

    @Override
    public abstract void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value);

    @Override
    public abstract void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value);
}
