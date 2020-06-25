package lsfusion.gwt.client.form.object.table.tree.view;

import java.util.HashMap;
import java.util.Map;

public class GTreeColumnValue {
    private int level;
    private Boolean open;
    private boolean openDotBottom = true;
    private String sID;
    private Map<Integer, Boolean> lastInLevelMap = new HashMap<>();

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

    public boolean isOpenDotBottom() {
        return openDotBottom;
    }

    public void setOpenDotBottom(boolean openDotBottom) {
        this.openDotBottom = openDotBottom;
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
        this.lastInLevelMap = new HashMap<>(lastInLevelMap);
    }

    public void addLastInLevel(int level, boolean last) {
        lastInLevelMap.put(level, last);
    }

    public boolean isLastInLevel(int level) {
        Boolean last = lastInLevelMap.get(level);
        return last != null && last;
    }
}
