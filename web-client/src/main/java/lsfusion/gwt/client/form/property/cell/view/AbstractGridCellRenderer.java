package lsfusion.gwt.client.form.property.cell.view;

import lsfusion.gwt.client.ClientMessages;

public abstract class AbstractGridCellRenderer implements GridCellRenderer {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    protected final String EMPTY_VALUE = messages.formRendererNotDefined();
    protected final String REQUIRED_VALUE = messages.formRendererRequired();
}
