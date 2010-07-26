package platform.client.form.grid;

import platform.client.form.ClientForm;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.queries.FilterController;
import platform.client.form.queries.FindController;
import platform.client.logics.*;
import platform.interop.Order;
import platform.interop.form.screen.ExternalScreenComponent;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridController {

    private final ClientGridView view;

    private final GridView gridView;
    public JComponent getView() {
        return gridView;
    }

    private final GridTable gridTable;

    private final ClientForm form;

    private final GroupObjectLogicsSupplier logicsSupplier;

    public GridController(ClientGridView iview, GroupObjectLogicsSupplier ilogicsSupplier, ClientForm iform) {

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

        gridView = new GridView(logicsSupplier, form, view.showFind ? findController.getView() : null, view.showFilter ? filterController.getView() : null) {

            protected void needToBeShown() {
                hidden = false;
                showViews();
            }

            protected void needToBeHidden() {
                hidden = true;
                hideViews();
            }
        };
        gridTable = gridView.getTable();

        if (view.minRowCount > 0) { // вообще говоря, так делать неправильно, посколько и HeaderHeight и RowHeight могут изменяться во времени
            Dimension minSize = gridTable.getMinimumSize();
            minSize.height = Math.max(minSize.height, (int)gridTable.getTableHeader().getPreferredSize().getHeight() + view.minRowCount * gridTable.getRowHeight());
            gridView.setMinimumSize(minSize);
        }
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(view, gridView);
        for (Map.Entry<ClientCellView, ExternalScreenComponent> entry : extViews.entrySet()) {
            entry.getKey().externalScreen.add(form.getID(), entry.getValue(), entry.getKey().externalScreenConstraints);
        }
    }

    public void addGroupObjectCells() {
//                System.out.println("addGroupObjectCells");
        for (ClientObjectImplementView object : logicsSupplier.getGroupObject()) {
            if (object.objectCellView.show)
               gridTable.addColumn(object.objectCellView);
            if (object.classCellView.show)
                gridTable.addColumn(object.classCellView);
        }

        // здесь еще добавить значения идентификаторов
        fillTableObjectID();

        gridTable.updateTable();
    }

    public void removeGroupObjectCells() {
//                System.out.println("removeGroupObjectCells");
        for (ClientObjectImplementView object : logicsSupplier.getGroupObject()) {
            if(object.objectCellView.show)
                gridTable.removeColumn(object.objectCellView);
            gridTable.removeColumn(object.classCellView);
        }
        gridTable.updateTable();
    }

    private Map<ClientCellView, ExternalScreenComponent> extViews = new HashMap<ClientCellView, ExternalScreenComponent>();

    private void addExternalScreenComponent(ClientCellView key) {
        if (!extViews.containsKey(key)) {
            ExternalScreenComponent extView = new ExternalScreenComponent();
            extViews.put(key, extView);
        }
    }

    public void addProperty(ClientPropertyView property) {
//                System.out.println("addProperty " + property.toString());
        if (gridTable.addColumn(property))
            gridTable.updateTable();

        if (property.externalScreen != null) {
            addExternalScreenComponent(property);
        }
    }

    public void removeProperty(ClientPropertyView property) {
//                System.out.println("removeProperty " + property.toString());
        if (gridTable.removeColumn(property))
            gridTable.updateTable();
    }

    public void setGridObjects(List<ClientGroupObjectValue> gridObjects) {
        gridTable.setGridObjects(gridObjects);

        //здесь еще добавить значения идентификаторов
        fillTableObjectID();
    }

    public void setGridClasses(List<ClientGroupObjectClass> gridClasses) {
        fillTableObjectClasses(gridClasses);
    }

    public void selectObject(ClientGroupObjectValue currentObject) {
        gridTable.selectObject(currentObject);
    }

    public void setPropertyValues(ClientPropertyView property, Map<ClientGroupObjectValue, Object> values) {
        gridTable.setColumnValues(property, values);
        if (extViews.containsKey(property)) {
            Object value = getSelectedValue(property);
            extViews.get(property).setValue((value == null) ? "" : value.toString());
            property.externalScreen.invalidate();
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

    boolean hidden = false;
    public void hideViews() {
        gridView.setVisible(false);
    }

    public void showViews() {
        if (!hidden)
            gridView.setVisible(true);
    }

    private void fillTableObjectID() {

        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.objectCellView.show) {
                Map<ClientGroupObjectValue, Object> values = new HashMap<ClientGroupObjectValue, Object>();
                for (ClientGroupObjectValue value : gridTable.getGridRows())
                    values.put(value, value.get(object));
                gridTable.setColumnValues(object.objectCellView, values);
            }
    }

    private void fillTableObjectClasses(List<ClientGroupObjectClass> classes) {

        for (ClientObjectImplementView object : logicsSupplier.getGroupObject()) {

            Map<ClientGroupObjectValue, Object> cls = new HashMap<ClientGroupObjectValue, Object>();

            List<ClientGroupObjectValue> gridRows = gridTable.getGridRows();
            for (int i = 0; i < gridRows.size(); i++) {
                cls.put(gridRows.get(i), classes.get(i).get(object));
            }

            gridTable.setColumnValues(object.classCellView, cls);
        }
    }
}
