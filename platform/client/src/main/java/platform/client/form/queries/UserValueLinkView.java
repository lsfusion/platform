package platform.client.form.queries;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.cell.CellTable;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.ClientUserValueLink;

import javax.swing.*;
import java.awt.*;

class UserValueLinkView extends ValueLinkView {

    private final ClientUserValueLink valueLink;
    private ClientPropertyDraw property;

    private final CellTable valueView;

    // нужен для получения текущих значений в таблице
    private final GroupObjectLogicsSupplier logicsSupplier;

    public UserValueLinkView(ClientUserValueLink ivalueLink, ClientPropertyDraw iproperty, GroupObjectLogicsSupplier ilogicsSupplier) {
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

            public ClientPropertyDraw getProperty(int col) {
                return property;
            }

            public Object getHighlightValue(int row) {
                return null;
            }

            public Color getHighlightColor() {
                return null;
            }

            public ClientFormController getForm() {
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

    public void propertyChanged(ClientPropertyDraw iproperty) {

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