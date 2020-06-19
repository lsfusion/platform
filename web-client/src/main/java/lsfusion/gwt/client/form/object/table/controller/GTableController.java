package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.property.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface GTableController {
    void updateCellBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values);
    void updateCellForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values);
    void updatePropertyCaptions(GCaptionReader reader, Map<GGroupObjectValue, Object> values);
    void updateShowIfValues(GShowIfReader reader, Map<GGroupObjectValue, Object> values);
    void updateFooterValues(GFooterReader reader, Map<GGroupObjectValue, Object> values);
    void updateReadOnlyValues(GReadOnlyReader reader, Map<GGroupObjectValue, Object> values);
    void updateLastValues(GLastReader reader, Map<GGroupObjectValue, Object> values);
    void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values);
    void updateRowForegroundValues(Map<GGroupObjectValue, Object> values);

    GGroupObjectValue getCurrentKey();
    GGroupObject getSelectedGroupObject();
    List<GPropertyDraw> getGroupObjectProperties();
    List<GObject> getObjects();
    List<GPropertyDraw> getPropertyDraws();
    GPropertyDraw getSelectedProperty();
    GGroupObjectValue getSelectedColumn();
    Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey);

    GFormController getForm();

    void setContainerCaption(GContainer container, String caption);

    boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet);
}
