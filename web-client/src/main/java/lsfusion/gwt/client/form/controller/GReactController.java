package lsfusion.gwt.client.form.controller;

import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.view.GReactFormData;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GFormGroupController;
import lsfusion.gwt.client.form.object.table.controller.GFormPropertyController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

import java.util.ArrayList;

public class GReactController implements GFormGroupController, GFormPropertyController {
    private final GGroupObject group;
    private final GReactFormData reactData;
    private final Runnable refreshOptimistic;

    public GReactController(GGroupObject group, GReactFormData reactData, Runnable refreshOptimistic) {
        this.group = group;
        this.reactData = reactData;
        this.refreshOptimistic = refreshOptimistic;
    }

    @Override
    public GGroupObjectValue getSelectedKey() {
        return reactData.getCurrentObject(group);
    }

    @Override
    public int getSelectedRow() {
        return reactData.getRowIndex(group, getSelectedKey());
    }

    @Override
    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {
        if (reactData.modifyGroupObject(group, key, add, position))
            refreshOptimistic.run();
    }

    @Override
    public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) {
    }

    @Override
    public void updateCurrentKey(GGroupObjectValue currentKey) {
    }

    @Override
    public boolean isPropertyShown(GPropertyDraw property) {
        return true;
    }

    @Override
    public void focusProperty(GPropertyDraw property) {
    }

    @Override
    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
        GGroupObjectValue cellKey = property.isList
                ? (fullCurrentKey.isEmpty() ? getSelectedKey() : property.groupObject.filterRowKeys(fullCurrentKey))
                : property.filterColumnKeys(fullCurrentKey);
        if (cellKey == null)
            return null;

        PValue oldValue = reactData.getValue(property, cellKey);
        if (reactData.setPropertyValue(property, cellKey, value))
            refreshOptimistic.run();
        return new Pair<>(cellKey, oldValue);
    }

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
    }
}
