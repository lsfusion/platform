package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.client.MainFrameMessages;

public abstract class AbstractGridCellRenderer implements GridCellRenderer {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();
    protected final String EMPTY_VALUE = messages.formRendererNotDefined();
    protected final String REQUIRED_VALUE = messages.formRendererRequired();

    @Override
    public abstract void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value);

    @Override
    public abstract void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value);
}
