package platform.client.form.grid;

import platform.base.BaseUtils;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import java.util.*;

public class KeyController {
    private final GridTable table;
    public boolean isRecording = false;
    private Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> values = new LinkedHashMap<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>>();

    public KeyController(GridTable table) {
        this.table = table;
    }

    public Set<ClientGroupObjectValue> getKeys() {
        return values.keySet();
    }

    public Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> getValues() {
        return values;
    }

    public void startRecording(int firstIndex) {
        if (firstIndex == -1)
            return;
        if (!isRecording) {
            values.clear();
            values.put(table.getRowKeys().get(firstIndex), getRowData(table, firstIndex));
        }
        isRecording = true;
    }

    public void stopRecording() {
        isRecording = false;
    }

    public void completeRecording(int lastIndex) {
        if (isRecording && lastIndex != -1 && !values.isEmpty())
            record(table.getRowKeys().indexOf(BaseUtils.lastSetElement(values.keySet())) < lastIndex, lastIndex);
    }

    public static Map<ClientPropertyDraw, Object> getRowData(GridTable table, int rowIndex) {
        Map<ClientPropertyDraw, Object> map = new HashMap<ClientPropertyDraw, Object>();
        for (ClientPropertyDraw property : table.getVisibleProperties()) {
            map.put(property, table.getValueAt(rowIndex, table.getModel().getPropertyIndex(property, null)));
        }
        return map;
    }

    public void record(boolean down, int index) {
        if (!isRecording) return;

        int previousRow = table.getSelectedRow();
        if (index != -1 && previousRow != -1) {
            int start = values.isEmpty() ? previousRow : table.getRowKeys().indexOf(BaseUtils.lastSetElement(values.keySet()));
            start = down ? start + 1 : start - 1;

            Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> segment = new LinkedHashMap<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>>();
            for (int i = start; (down && i <= index) || (!down && i >= index); i = (down ? i + 1 : i - 1)) {
                segment.put(table.getRowKeys().get(i), getRowData(table, i));
            }

            values = mergeSelectionMaps(values, segment);
        }
    }

    public void clear() {
        values.clear();
    }

    public static Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> mergeSelectionMaps(Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> primary, Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> secondary) {
        boolean intersect = false;
        for (ClientGroupObjectValue value : secondary.keySet()) {
            if (primary.keySet().contains(value)) {
                intersect = true;
                break;
            }
        }

        if (intersect) {
            if (primary.keySet().containsAll(secondary.keySet())) {
                primary.remove(BaseUtils.lastSetElement(primary.keySet()));
                secondary.remove(BaseUtils.lastSetElement(secondary.keySet()));
                for (ClientGroupObjectValue key : secondary.keySet()) {
                    primary.remove(key);
                }
            } else {
                for (ClientGroupObjectValue key : primary.keySet()) {
                    secondary.remove(key);
                }
                Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> tmp = new LinkedHashMap<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>>();
                tmp.put(primary.keySet().iterator().next(), primary.values().iterator().next());
                tmp.putAll(secondary);
                primary = tmp;
            }
        } else {
            primary.putAll(secondary);
        }
        return primary;
    }
}
