package platform.client.form.queries;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.cell.CellTable;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.ClientUserValueLink;

import java.awt.*;
import java.util.EventObject;

public class UserValueLinkView extends ValueLinkView {

    private final ClientUserValueLink valueLink;
    private ClientPropertyDraw property;

    private final CellTable valueTable;

    // нужен для получения текущих значений в таблице
    private final GroupObjectLogicsSupplier logicsSupplier;

    public UserValueLinkView(ClientUserValueLink ivalueLink, ClientPropertyDraw iproperty, GroupObjectLogicsSupplier ilogicsSupplier) {
        super();

        valueLink = ivalueLink;
        property = iproperty;
        logicsSupplier = ilogicsSupplier;

        // непосредственно объект для изменения значения свойств
        valueTable = new CellTable(false, new ClientGroupObjectValue()) {

            protected boolean cellValueChanged(Object value, boolean aggValue) {

                UserValueLinkView.this.setValue(value);
                if (listener != null)
                    listener.valueChanged();

                return true;
            }

            public boolean isDataChanging() {
                return false;
            }

            public boolean isPressed(int row, int column) {
                return false;
            }

            public ClientPropertyDraw getProperty() {
                return property;
            }

            public Color getBackgroundColor(int row, int column) {
                return null;
            }

            public ClientFormController getForm() {
                return logicsSupplier.getForm();
            }

            @Override
            public boolean editCellAt(int row, int column, EventObject e) {
                boolean edition = super.editCellAt(row, column, e);
                if (edition)
                    listener.valueChanged();
                return edition;
            }
        };

        // приходится в явную указывать RowHeight, поскольку это JTable и он сам не растянется
        valueTable.setRowHeight(QueryConditionView.PREFERRED_HEIGHT);
        add(valueTable, BorderLayout.CENTER);
    }

    public boolean requestFocusInWindow() {
        return valueTable.requestFocusInWindow();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(30, getPreferredSize().height);
    }

    public void propertyChanged(ClientPropertyDraw iproperty) {
        property = iproperty;

        valueTable.keyChanged(property);
        
        setValue(logicsSupplier.getSelectedValue(property, null));
    }

    public void startEditing() {
        valueTable.editCellAt(0, 0);
        Component editor = valueTable.getEditorComponent();
        if (editor != null) {
            editor.requestFocusInWindow();
        }
    }

    public void stopEditing() {
        valueTable.stopEditing();
    }

    void setValue(Object value) {
        valueLink.value = value;
        valueTable.setValue(value);
    }
}