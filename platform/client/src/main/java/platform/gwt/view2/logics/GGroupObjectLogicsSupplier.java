package platform.gwt.view2.logics;

import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.reader.GBackgroundReader;
import platform.gwt.view2.reader.GCaptionReader;
import platform.gwt.view2.reader.GFooterReader;
import platform.gwt.view2.reader.GForegroundReader;

import java.util.Map;

public interface GGroupObjectLogicsSupplier {
    void updatePropertyDrawValues(GPropertyDraw reader, Map<GGroupObjectValue, Object> values);
    void updateBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values);
    void updateForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values);
    void updateCaptionValues(GCaptionReader reader, Map<GGroupObjectValue, Object> values);
    void updateFooterValues(GFooterReader reader, Map<GGroupObjectValue, Object> values);
    void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values);
    void updateRowForegroundValues(Map<GGroupObjectValue, Object> values);
}
