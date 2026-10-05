package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GGroupObjectPropertyReader;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.LinkedHashMap;

// THE FORM'S CONTROLLER OF A GROUP, settled once where the design says who draws its rows
// (GFormController.groupControllers): the platform's grid or tree, or the node React draws the rows on - all the form
// sends for the group goes to it
public interface GGroupController {
    GGroupObjectValue getSelectedKey();
    int getSelectedRow();
    void modifyGroupObject(GGroupObjectValue key, boolean add, int position);
    void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex);
    void updateCurrentKey(GGroupObjectValue currentKey);
    // the current object as the form changes it, before the server answers - which the platform's own view has chosen
    // itself
    void changeCurrentKey(GGroupObjectValue currentKey);
    // what a reader of the group brings (GGroupObjectPropertyReader): an attribute of its rows or of the group
    void updateAttribute(GGroupObjectPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);
    // the group's user orders as the form has them - as a client set them or as the server reported them -: shown, not
    // sent; true - ascending
    void updateOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders);
    // the group's user filters as the server reports them, which it has applied already: shown, not sent
    void updateFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters);
}
