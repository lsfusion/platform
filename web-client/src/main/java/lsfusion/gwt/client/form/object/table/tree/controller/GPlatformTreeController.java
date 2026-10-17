package lsfusion.gwt.client.form.object.table.tree.controller;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPlatformTableController;
import lsfusion.gwt.client.form.object.table.controller.GToolbarController;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeGridRecord;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.List;

public final class GPlatformTreeController extends GPlatformTableController<GTreeGroup, GTreeTable, GToolbarController<GTreeController>> implements GTreeController {
    public GPlatformTreeController(GTreeGroup iTreeGroup, GFormController iFormController, GForm iForm, GToolbarController<GTreeController> toolbar) {
        super(iFormController, iTreeGroup, toolbar);

        initGridView();
        // the tree given to its toolbar - what opens it, the platform's buttons or a view's members -, as a table gives
        // itself to its own; the filter button, made once the tree is, goes before them all the same
        toolbar.addRows(this);
        view = new GTreeTable(iFormController, iForm, this, table, gridView);

        changeGridView(view);
    }

    public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) {
        view.setKeys(group, keys, fc.parentObjects.get(group), fc.expandables.get(group), requestIndex);
    }

    // shown while it has a column besides its hierarchical one
    @Override
    protected boolean updateView(long requestIndex, GFormChanges fc) {
        view.update();
        return view.getColumnCount() > 1;
    }

    // used in filters and user preferences
    @Override
    public GGroupObject getSelectedGroupObject() {
        GTreeGridRecord record = view.getSelectedRowValue();
        return record != null
                ? record.getGroup()
                : table.groups.get(0);
    }

    @Override
    public GPropertyDraw getSelectedFilterProperty() {
        return view.getSelectedFilterProperty();
    }

    @Override
    public GGroupObjectValue getSelectedColumnKey() {
        return GGroupObjectValue.EMPTY; // пока не поддерживаются группы в колонки
    }

    @Override
    public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return view.getSelectedValue(property);
    }

    @Override
    public List<Pair<Column, String>> getFilterColumns() {
        return view.getFilterColumns(getSelectedGroupObject());
    }

    // ===== what its toolbar asks of the tree (GTreeController): the selected node opened, and every node under it -
    // one with children -, or the tree, from the selected node's group down: the nodes of that group, under the nodes
    // open above it, and of the groups below
    @Override
    public void expandCurrent() {
        view.fireExpandNodeRecursive(true);
    }
    @Override
    public void expandTree() {
        view.fireExpandNodeRecursive(false);
    }
    // ... and what a view's part of its toolbar opens through members of its own: the tree itself
    // (GTreeController.getOpenedByToolbarMembers)
    @Override
    public GTreeController getOpenedByToolbarMembers() {
        return this;
    }
}
