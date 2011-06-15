package platform.gwt.form.client.ui;

import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GridDataRecord extends ListGridRecord {
    public final GGroupObjectValue key;

    public GridDataRecord(GGroupObjectValue key, HashMap<GPropertyDraw, Object> values) {
        this.key = key;
        if (values != null) {
            for (Map.Entry<GPropertyDraw, Object> entry : values.entrySet()) {
                //чтобы задавать всё, как простой объект, а не конкретный класс
                JSOHelper.setAttribute(jsObj, entry.getKey().sID, entry.getValue());
//                setAttribute(entry.getKey().sID, entry.getValue());
            }
        }
    }

    public int getRecordKey() {
        return -1;
    }

    public static GridDataRecord[] createRecords(ArrayList<GGroupObjectValue> gridKeys, HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Object>> gridProps) {
        HashMap<GGroupObjectValue, HashMap<GPropertyDraw, Object>> values = new HashMap<GGroupObjectValue, HashMap<GPropertyDraw, Object>>();

        for (Map.Entry<GPropertyDraw, HashMap<GGroupObjectValue, Object>> entry : gridProps.entrySet()) {
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

        GridDataRecord result[] = new GridDataRecord[gridKeys.size()];
        int i = 0;
        for (GGroupObjectValue key : gridKeys) {
            result[i++] = new GridDataRecord(key, values.get(key));
        }

        return result;
    }
}
