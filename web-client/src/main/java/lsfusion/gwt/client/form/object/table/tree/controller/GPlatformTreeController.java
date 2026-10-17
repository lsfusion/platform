package lsfusion.gwt.client.form.object.table.tree.controller;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPlatformTableController;
import lsfusion.gwt.client.form.object.table.controller.GToolbarController;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeGridRecord;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.List;

public class GPlatformTreeController extends GPlatformTableController implements GTreeController {
    private final GTreeGroup treeGroup;

    private final GTreeTable tree;

    // the tree's toolbar, one for all its groups, of whichever kind it is, which the tree gives itself to as it is made
    private final GToolbarController<GTreeController> toolbar;

    public GPlatformTreeController(GTreeGroup iTreeGroup, GFormController iFormController, GForm iForm, GToolbarController<GTreeController> toolbar) {
        super(iFormController);
        treeGroup = iTreeGroup;
        this.toolbar = toolbar;

        initGridView();
        // the tree given to its toolbar - what opens it, the platform's buttons or a view's members -, as a table gives
        // itself to its own; the filter button, made once the tree is, goes before them all the same
        toolbar.addRows(this);
        tree = new GTreeTable(iFormController, iForm, this, treeGroup, gridView);

        changeGridView(tree);
    }

    @Override
    protected GTreeTable getView() {
        return tree;
    }

    @Override
    protected GToolbarController<GTreeController> getToolbar() {
        return toolbar;
    }

    public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) {
        tree.setKeys(group, fc.gridObjects.get(group), fc.parentObjects.get(group), fc.expandables.get(group), requestIndex);
    }

    // shown while it has a column besides its hierarchical one
    @Override
    protected boolean updateView(long requestIndex, GFormChanges fc) {
        tree.update();
        return tree.getColumnCount() > 1;
    }

    // used in filters and user preferences
    @Override
    public GGroupObject getSelectedGroupObject() {
        GTreeGridRecord record = tree.getSelectedRowValue();
        return record != null
                ? record.getGroup()
                : treeGroup.groups.get(0);
    }

    @Override
    public GPropertyDraw getSelectedFilterProperty() {
        return tree.getSelectedFilterProperty();
    }

    @Override
    public GGroupObjectValue getSelectedColumnKey() {
        return GGroupObjectValue.EMPTY; // пока не поддерживаются группы в колонки
    }

    @Override
    public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return tree.getSelectedValue(property);
    }

    @Override
    public List<Pair<Column, String>> getFilterColumns() {
        return tree.getFilterColumns(getSelectedGroupObject());
    }

    @Override
    public GGridProperty getGridComponent() {
        return treeGroup;
    }

    // ===== what its toolbar asks of the tree (GTreeController): the selected node opened, and every node under it -
    // one with children -, or the tree, from the selected node's group down: the nodes of that group, under the nodes
    // open above it, and of the groups below
    @Override
    public void expandCurrent() {
        tree.fireExpandNodeRecursive(true);
    }
    @Override
    public void expandTree() {
        tree.fireExpandNodeRecursive(false);
    }
    // ... and what a view's part of its toolbar opens through members of its own: the tree itself
    // (GTreeController.getOpenedByToolbarMembers)
    @Override
    public GTreeController getOpenedByToolbarMembers() {
        return this;
    }
}
