package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.base.GwtClientUtils;

public class GTreeColumnValue {
    private int level;
    private Boolean open;
    private boolean openDotBottom = true;
    private boolean closedDotBottom;
    private String sID;
    private boolean[] lastInLevelMap;

    public GTreeColumnValue(int level, boolean[] lastInLevelMap, String sID) {
        this.level = level;
        this.sID = sID;
        this.lastInLevelMap = lastInLevelMap;
        assert lastInLevelMap.length == level;
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

    public boolean isClosedDotBottom() {
        return closedDotBottom;
    }

    public void setClosedDotBottom(boolean closedDotBottom) {
        this.closedDotBottom = closedDotBottom;
    }

    public String getSID() {
        return sID;
    }

    public void setSID(String sID) {
        this.sID = sID;
    }

    public boolean[] getLastInLevelMap() {
        return lastInLevelMap;
    }

    public void setLastInLevelMap(boolean[] lastInLevelMap) {
        this.lastInLevelMap = lastInLevelMap;
    }

    public boolean isLastInLevel(int level) {
        return lastInLevelMap[level];
    }

    public boolean equalsValue(GTreeColumnValue that) {
        if(!(level == that.level &&
                openDotBottom == that.openDotBottom &&
                closedDotBottom == that.closedDotBottom &&
                GwtClientUtils.nullEquals(open, that.open) &&
                sID.equals(that.sID)))
            return false;

        for (int i=0; i<lastInLevelMap.length; i++)
            if (lastInLevelMap[i] != that.lastInLevelMap[i])
                return false;

        return true;
    }
}
