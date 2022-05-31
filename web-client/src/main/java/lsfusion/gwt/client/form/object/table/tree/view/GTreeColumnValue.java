package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.base.GwtClientUtils;

public class GTreeColumnValue {
    public final int level;
    public final Boolean open;
    public final boolean openDotBottom;
    public final boolean closedDotBottom;
    public final String sID;
    public final boolean[] lastInLevelMap;

    public GTreeColumnValue(int level, boolean[] lastInLevelMap, Boolean open, String sID, boolean openDotBottom, boolean closedDotBottom) {
        this.level = level;
        this.sID = sID;
        this.lastInLevelMap = lastInLevelMap;
        this.open = open;
        this.openDotBottom = openDotBottom;
        this.closedDotBottom = closedDotBottom;
        assert lastInLevelMap.length == level;
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
