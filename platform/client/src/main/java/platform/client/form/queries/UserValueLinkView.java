package platform.client.form.queries;

import platform.client.form.ClientForm;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.cell.CellTable;
import platform.client.logics.ClientCellView;
import platform.client.logics.ClientPropertyView;
import platform.client.logics.ClientUserValueLink;

import javax.swing.*;
import java.awt.*;

class UserValueLinkView extends ValueLinkView {

    private final ClientUserValueLink valueLink;
    private ClientPropertyView property;

    private final CellTable valueView;

    // нужен для получения текущих значений в таблице
    private final GroupObjectLogicsSupplier logicsSupplier;

    public UserValueLinkView(ClientUserValueLink ivalueLink, ClientPropertyView iproperty, GroupObjectLogicsSupplier ilogicsSupplier) {
        super();

        valueLink = ivalueLink;
        property = iproperty;
        logicsSupplier = ilogicsSupplier;

        JComboBox compBorder = new JComboBox();
        setBorder(compBorder.getBorder());

        // непосредственно объект для изменения значения свойств
        valueView = new CellTable(false) {

            protected boolean cellValueChanged(Object value) {

                UserValueLinkView.this.setValue(value);
                if (listener != null)
                    listener.valueChanged();

                return true;
            }

            public boolean isDataChanging() {
                return false;
            }

            public ClientCellView getCellView(int col) {
                return property;
            }

            public ClientForm getForm() {
                return logicsSupplier.getForm();
            }

        };

        // приходится в явную указывать RowHeight, поскольку это JTable и он сам не растянется
        valueView.setRowHeight(QueryConditionView.PREFERRED_HEIGHT);
        add(valueView, BorderLayout.CENTER);
    }

    public boolean requestFocusInWindow() {
        return valueView.requestFocusInWindow();
    }

    public void propertyChanged(ClientPropertyView iproperty) {

        property = iproperty;

        valueView.keyChanged(property);
        
        setValue(logicsSupplier.getSelectedValue(property));
    }

    public void stopEditing() {
        valueView.stopEditing();
    }

    void setValue(Object value) {
        valueLink.value = value;
        valueView.setValue(value);
    }
}