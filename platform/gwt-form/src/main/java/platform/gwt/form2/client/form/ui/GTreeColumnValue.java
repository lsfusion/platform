package platform.gwt.form2.client.form.ui;

import java.util.HashMap;
import java.util.Map;

public class GTreeColumnValue {
    private int level;
    private Boolean open;
    private int index;
    private Map<Integer, Boolean> lastInLevelMap = new HashMap<Integer, Boolean>();

    public GTreeColumnValue(int level, int index) {
        this.level = level;
        this.index = index;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Map<Integer, Boolean> getLastInLevelMap() {
        return lastInLevelMap;
    }

    public void setLastInLevelMap(Map<Integer, Boolean> lastInLevelMap) {
        this.lastInLevelMap = new HashMap<Integer, Boolean>(lastInLevelMap);
    }

    public void addLastInLevel(int level, boolean last) {
        lastInLevelMap.put(level, last);
    }

    public boolean isLastInLevel(int level) {
        Boolean last = lastInLevelMap.get(level);
        return last != null && last;
    }
}
