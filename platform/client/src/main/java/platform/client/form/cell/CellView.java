package platform.client.form.cell;

import platform.client.logics.ClientCellView;
import platform.client.form.ClientForm;

import javax.swing.*;
import java.awt.*;

abstract class CellView extends JPanel {

    private JLabel label;
    private CellTable table;

    protected abstract ClientCellView getKey();

    public int getID() { return getKey().getID(); }

    @Override
    public int hashCode() {
        return getID();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CellView && ((CellView) o).getID() == this.getID();
    }

    public CellView() {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        label = new JLabel();
        label.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
        add(label);

        table = new CellTable() {

            protected boolean cellValueChanged(Object value) {
                return CellView.this.cellValueChanged(value);
            }

            public boolean isDataChanging() {
                return CellView.this.isDataChanging();
            }

            public ClientCellView getCellView(int col) {
                return getKey();
            }

            public ClientForm getForm() {
                return CellView.this.getForm();
            }
        };
        table.setBorder(BorderFactory.createLineBorder(Color.gray));

        add(table);

    }

    protected abstract boolean cellValueChanged(Object value);
    protected abstract boolean isDataChanging();
    protected abstract ClientForm getForm();

    @Override
    public boolean requestFocusInWindow() {
        return table.requestFocusInWindow();
    }

    void keyChanged(ClientCellView key) {

        label.setText(key.caption);
        table.keyChanged(key);
    }

    void setValue(Object ivalue) {
        table.setValue(ivalue);
    }
}