package lsfusion.gwt.client.form.property.cell;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.ui.cellview.DataGrid;
import lsfusion.gwt.client.base.ui.cellview.cell.Cell;

public abstract class AbstractGridCellRenderer implements GridCellRenderer {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    protected final String EMPTY_VALUE = messages.formRendererNotDefined();
    protected final String REQUIRED_VALUE = messages.formRendererRequired();

    @Override
    public abstract void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value);

    @Override
    public abstract void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value);
}
