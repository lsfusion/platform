package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Function;

public abstract class GPropertyController {
    public abstract void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values);

    public abstract void setContainerCaption(GContainer container, String caption);

    public abstract void updateProperty(GGroupObject group, GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void removeProperty(GGroupObject group, GPropertyDraw property);

    protected void processFormChanges(GGroupObject group, GFormChanges fc, NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects,
                                      Function<GPropertyDraw, Boolean> checkRemoveProperty,
                                      Function<GPropertyDraw, Boolean> checkUpdateProperty,
                                      Function<GPropertyReader, Boolean> checkPropertyReader) {

        for (GPropertyDraw property : fc.dropProperties) {
            if (checkRemoveProperty.apply(property)) { // drop properties sent without checking if it was sent for update at least once, so it's possible when drop is sent for property that has not been added
                removeProperty(group, property);
            }
        }

        // first proceed property with its values, then extra values
        fc.properties.foreachEntry((key, value) -> {
            if (key instanceof GPropertyDraw) {
                GPropertyDraw property = (GPropertyDraw) key;
                if (checkUpdateProperty.apply(property)) // filling keys
                    updateProperty(group, property, getColumnKeys(property, currentGridObjects), fc.updateProperties.contains(property), value);
            }
        });

        fc.properties.foreachEntry((key, value) -> {
            if (!(key instanceof GPropertyDraw)) {
                if (checkPropertyReader.apply(key)) {
                    key.update(this, value, false);
                }
            }
        });
    }

    private ArrayList<GGroupObjectValue> getColumnKeys(GPropertyDraw property, NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects) {
        ArrayList<GGroupObjectValue> columnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
        if (property.columnGroupObjects != null) {
            LinkedHashMap<GGroupObject, ArrayList<GGroupObjectValue>> groupColumnKeys = new LinkedHashMap<>();
            for (GGroupObject columnGroupObject : property.columnGroupObjects) {
                ArrayList<GGroupObjectValue> columnGroupKeys = currentGridObjects.get(columnGroupObject);
                if (columnGroupKeys != null) {
                    groupColumnKeys.put(columnGroupObject, columnGroupKeys);
                }
            }

            columnKeys = GGroupObject.mergeGroupValues(groupColumnKeys);
        }
        return columnKeys;
    }
}
