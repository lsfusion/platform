package platform.gwt.form2.client.form.ui;

import java.util.HashMap;
import java.util.Map;

public class GTreeColumnValue {
    private int level;
    private Boolean open;
    private String sID;
    private Map<Integer, Boolean> lastInLevelMap = new HashMap<Integer, Boolean>();

    public GTreeColumnValue(int level, String sID) {
        this.level = level;
        this.sID = sID;
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

    public String getSID() {
        return sID;
    }

    public void setSID(String sID) {
        this.sID = sID;
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
