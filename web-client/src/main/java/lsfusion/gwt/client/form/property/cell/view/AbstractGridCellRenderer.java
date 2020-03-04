package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.grid.DataGrid;

public abstract class AbstractGridCellRenderer implements GridCellRenderer {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    protected final String EMPTY_VALUE = messages.formRendererNotDefined();
    protected final String REQUIRED_VALUE = messages.formRendererRequired();

    @Override
    public abstract void renderDom(DataGrid table, DivElement cellElement, Object value);

    @Override
    public abstract void renderDom(Element cellElement, Object value);

    @Override
    public abstract void updateDom(DivElement cellElement, DataGrid table, Object value);

    @Override
    public abstract void updateDom(Element cellElement, Object value);
}
