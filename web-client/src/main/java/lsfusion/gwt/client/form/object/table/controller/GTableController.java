package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.view.Column;

import java.util.LinkedHashMap;
import java.util.List;

public interface GTableController {
    void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values);
    void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values);

    GGroupObjectValue getCurrentKey();
    GGroupObject getSelectedGroupObject();
    List<GPropertyDraw> getGroupObjectProperties();
    List<GObject> getObjects();
    List<GPropertyDraw> getPropertyDraws();
    GPropertyDraw getSelectedProperty();
    GGroupObjectValue getSelectedColumnKey();
    Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey);
    List<Pair<Column, String>> getSelectedColumns();

    GFormController getForm();

    void setContainerCaption(GContainer container, String caption);

    boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet);
}
