package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GGroupObjectPropertyReader;
import lsfusion.gwt.client.form.property.PValue;

import java.util.ArrayList;

// THE FORM'S CONTROLLER OF A GROUP, settled once where the design says who draws its rows
// (GFormController.groupControllers): the platform's grid or tree, or the rows' part where React draws them - all the
// form sends for the group goes to it, but for what is said apart: what shows its user orders and its user filters
// (GUserOrdersController, GUserFiltersController)
public interface GGroupController {
    GGroupObjectValue getSelectedKey();
    int getSelectedRow();
    void modifyGroupObject(GGroupObjectValue key, boolean add, int position);
    void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex);
    void updateCurrentKey(GGroupObjectValue currentKey);
    // the current object as the form changes it, before the server answers - nothing to show where the platform
    // draws the group: its own view has chosen it itself, and a PANEL group has no row to move to
    default void changeCurrentKey(GGroupObjectValue currentKey) {
    }
    // ... and a node of a tree asked to open or close, with the index of its request: shown as asked until the keys of
    // the answer come with that index - nothing to show where the platform draws the tree, which has done it itself
    // (GTreeTable.expandNode), nor where the group is no tree's: a grid's rows and a PANEL group have no nodes
    default void changeExpanded(GGroupObjectValue key, boolean open, long requestIndex) {
    }
    // ... and every node of the group and of the groups below it
    default void changeExpandedAll(boolean open, long requestIndex) {
    }
    // ... and a node and every node under it, opened - a tree's toolbar only opens
    default void changeExpandedUnder(GGroupObjectValue key, long requestIndex) {
    }
    // what a reader of the group brings (GGroupObjectPropertyReader): an attribute of its rows or of the group
    void updateAttribute(GGroupObjectPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);
}
