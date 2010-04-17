package platform.client.form.grid;

import platform.client.form.LogicsSupplier;
import platform.client.form.ClientFormLayout;
import platform.client.form.ClientForm;
import platform.client.form.queries.FindController;
import platform.client.form.queries.FilterController;
import platform.client.logics.*;
import platform.interop.Order;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GridController {

    private final ClientGridView view;

    private final GridView gridView;
    public JComponent getView() {
        return gridView;
    }

    private final GridTable gridTable;

    ClientForm form;

    LogicsSupplier logicsSupplier;

    public GridController(ClientGridView iview, LogicsSupplier ilogicsSupplier, ClientForm iform) {

        view = iview;
        logicsSupplier = ilogicsSupplier;
        form = iform;

        FindController findController = new FindController(logicsSupplier) {

            protected boolean queryChanged() {

                form.changeFind(getConditions());

                gridTable.requestFocusInWindow();
                return true;
            }
        };

        FilterController filterController = new FilterController(logicsSupplier) {

            protected boolean queryChanged() {

                try {
                    form.changeFilter(logicsSupplier.getGroupObject(), getConditions());
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при применении фильтра", e);
                }

                gridTable.requestFocusInWindow();
                return true;
            }
        };

        gridView = new GridView(logicsSupplier, form, findController.getView(), filterController.getView());
        gridTable = gridView.getTable();

    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(view, gridView);
    }

    public void addGroupObjectID() {
//                System.out.println("addGroupObjectID");
        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.objectIDView.show)
               gridTable.addColumn(object.objectIDView);

        // здесь еще добавить значения идентификаторов
        fillTableObjectID();

        gridTable.updateTable();
    }

    public void removeGroupObjectID() {
//                System.out.println("removeGroupObjectID");
        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.objectIDView.show)
                gridTable.removeColumn(object.objectIDView);
        gridTable.updateTable();
    }

    public void addProperty(ClientPropertyView property) {
//                System.out.println("addProperty " + property.toString());
        if (gridTable.addColumn(property))
            gridTable.updateTable();
    }

    public void removeProperty(ClientPropertyView property) {
//                System.out.println("removeProperty " + property.toString());
        if (gridTable.removeColumn(property))
            gridTable.updateTable();
    }

    public void setGridObjects(List<ClientGroupObjectValue> igridObjects) {
        gridTable.setGridObjects(igridObjects);

        //здесь еще добавить значения идентификаторов
        fillTableObjectID();
    }

    public void selectObject(ClientGroupObjectValue currentObject) {
        gridTable.selectObject(currentObject);
    }

    public void setPropertyValues(ClientPropertyView property, Map<ClientGroupObjectValue, Object> values) {
        gridTable.setColumnValues(property, values);
    }

    private void fillTableObjectID() {
        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.objectIDView.show) {
                Map<ClientGroupObjectValue, Object> values = new HashMap<ClientGroupObjectValue, Object>();
                for (ClientGroupObjectValue value : gridTable.getGridRows())
                    values.put(value, value.get(object));
                gridTable.setColumnValues(object.objectIDView, values);
            }
    }

    public void changeGridOrder(ClientCellView property, Order modiType) throws IOException {
        gridTable.changeGridOrder(property, modiType);
    }

    public ClientCellView getCurrentCell() {
        return gridTable.getCurrentCell();
    }

    public Object getSelectedValue(ClientPropertyView cell) {
        return gridTable.getSelectedValue(cell);
    }

    public boolean requestFocusInWindow() {
        return gridTable.requestFocusInWindow();
    }

}
