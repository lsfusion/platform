package platform.gwt.view.logics;

import platform.gwt.view.*;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.reader.GBackgroundReader;
import platform.gwt.view.reader.GCaptionReader;
import platform.gwt.view.reader.GFooterReader;
import platform.gwt.view.reader.GForegroundReader;

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
