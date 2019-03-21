package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.util.List;
import java.util.Map;

public interface GTableController {
    void updatePropertyDrawValues(GPropertyDraw reader, Map<GGroupObjectValue, Object> values, boolean updateKeys);
    void updateBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values);
    void updateForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values);
    void updateCaptionValues(GCaptionReader reader, Map<GGroupObjectValue, Object> values);
    void updateShowIfValues(GShowIfReader reader, Map<GGroupObjectValue, Object> values);
    void updateFooterValues(GFooterReader reader, Map<GGroupObjectValue, Object> values);
    void updateReadOnlyValues(GReadOnlyReader reader, Map<GGroupObjectValue, Object> values);
    void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values);
    void updateRowForegroundValues(Map<GGroupObjectValue, Object> values);
    boolean hasPanelProperty(GPropertyDraw property);
    GGroupObjectValue getCurrentKey();
    void changeOrder(GPropertyDraw property, GOrder modiType);
    void clearOrders(GGroupObject groupObject);
    GGroupObject getSelectedGroupObject();
    List<GPropertyDraw> getGroupObjectProperties();
    List<GObject> getObjects();
    List<GPropertyDraw> getPropertyDraws();
    GPropertyDraw getSelectedProperty();
    GGroupObjectValue getSelectedColumn();
    Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey);
}
