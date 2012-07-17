package platform.gwt.view2;

import platform.gwt.view2.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GridDataRecord {
    private final HashMap<String, Object> values = new HashMap<String, Object>();

    public final GGroupObjectValue key;

    public GridDataRecord(GGroupObjectValue key, HashMap<GPropertyDraw, Object> values) {
        this.key = key;
        if (values != null) {
            for (Map.Entry<GPropertyDraw, Object> entry : values.entrySet()) {
                setAttribute(entry.getKey().sID, entry.getValue());
            }
        }
    }

    public void setAttribute(String name, Object value) {
        values.put(name, value);
    }

    public Object getAttribute(String name) {
        return values.get(name);
    }

    public static ArrayList<GridDataRecord> createRecords(ArrayList<GGroupObjectValue> gridKeys, Map<GPropertyDraw, Map<GGroupObjectValue, Object>> gridProps) {
        HashMap<GGroupObjectValue, HashMap<GPropertyDraw, Object>> values = new HashMap<GGroupObjectValue, HashMap<GPropertyDraw, Object>>();

        for (Map.Entry<GPropertyDraw, Map<GGroupObjectValue, Object>> entry : gridProps.entrySet()) {
            GPropertyDraw property = entry.getKey();
            for (Map.Entry<GGroupObjectValue, Object> e : entry.getValue().entrySet()) {
                GGroupObjectValue key = e.getKey();
                Object value = e.getValue();

                HashMap<GPropertyDraw, Object> keyMap = values.get(key);
                if (keyMap == null) {
                    keyMap = new HashMap<GPropertyDraw, Object>();
                    values.put(key, keyMap);
                }

                keyMap.put(property, value);
            }
        }

        ArrayList<GridDataRecord> result = new ArrayList<GridDataRecord>();
        for (GGroupObjectValue key : gridKeys) {
            result.add(new GridDataRecord(key, values.get(key)));
        }

        return result;
    }

}
