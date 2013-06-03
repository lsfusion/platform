package lsfusion.client.form.grid.groupchange;

import lsfusion.client.Main;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.grid.GridTable;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class GroupChangeAction extends AbstractAction {
    private GridTable gridTable;
    private ClientFormController form;

    public GroupChangeAction(GridTable igridTable) {
        gridTable = igridTable;
        form = gridTable.getForm();
    }

    public void actionPerformed(ActionEvent e) {
        if (true) {
            JOptionPane.showMessageDialog(null, "Reimplement group change");
            return;
        }

        int selectedColumn = gridTable.getSelectedColumn();
        if (selectedColumn < 0 || selectedColumn >= gridTable.getColumnCount()) {
            return;
        }

        ChangeDialog dlg = new ChangeDialog(Main.frame, gridTable);
        dlg.pack();
//        dlg.setResizable(false);

        if (dlg.prompt()) {
            ColumnProperty mainProperty = dlg.getMainProperty();
            ColumnProperty getterProperty = dlg.getGetterProperty();

//            try {
//                if (getterProperty != null) {
//                    form.groupChangePropertyDraw(mainProperty.property, mainProperty.columnKey, getterProperty.property, getterProperty.columnKey);
//                } else {
//                    form.changePropertyDraw(mainProperty.property, mainProperty.columnKey, dlg.getSelectedValue(), true, true);
//                }
//            } catch (IOException ioe) {
//                throw new RuntimeException(ClientResourceBundle.getString("form.grid.group.groupchange.error"), ioe);
//            }
        }
    }

}
