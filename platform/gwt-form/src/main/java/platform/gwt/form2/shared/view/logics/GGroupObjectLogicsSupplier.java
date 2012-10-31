package platform.gwt.form2.shared.view.logics;

import platform.gwt.form2.shared.view.GOrder;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.reader.GBackgroundReader;
import platform.gwt.form2.shared.view.reader.GCaptionReader;
import platform.gwt.form2.shared.view.reader.GFooterReader;
import platform.gwt.form2.shared.view.reader.GForegroundReader;

import java.util.Map;

public interface GGroupObjectLogicsSupplier {
    void updatePropertyDrawValues(GPropertyDraw reader, Map<GGroupObjectValue, Object> values, boolean updateKeys);
    void updateBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values);
    void updateForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values);
    void updateCaptionValues(GCaptionReader reader, Map<GGroupObjectValue, Object> values);
    void updateFooterValues(GFooterReader reader, Map<GGroupObjectValue, Object> values);
    void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values);
    void updateRowForegroundValues(Map<GGroupObjectValue, Object> values);
    boolean hasPanelProperty(GPropertyDraw property);
    GGroupObjectValue getCurrentKey();
    void changeOrder(GPropertyDraw property, GOrder modiType);
}
