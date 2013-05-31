package platform.gwt.form.shared.view.logics;

import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.shared.view.*;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.reader.*;

import java.util.List;
import java.util.Map;

public interface GGroupObjectLogicsSupplier {
    void updatePropertyDrawValues(GPropertyDraw reader, Map<GGroupObjectValue, Object> values, boolean updateKeys);
    void updateBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values);
    void updateForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values);
    void updateCaptionValues(GCaptionReader reader, Map<GGroupObjectValue, Object> values);
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
    Object getSelectedValue(GPropertyDraw property);
    void addFilterComponent(GRegularFilterGroup filterGroup, Widget filterWidget);
}
