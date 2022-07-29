package lsfusion.gwt.client.form.object.table.tree.view;

public class GTreeColumnValue {
    public final int level;

    public final GTreeColumnValueType type;

    public final boolean openDotBottom;
    public final boolean closedDotBottom;
    public final boolean[] lastInLevelMap;

    public GTreeColumnValue(int level, boolean[] lastInLevelMap, GTreeColumnValueType type, boolean openDotBottom, boolean closedDotBottom) {
        this.level = level;
        this.lastInLevelMap = lastInLevelMap;
        this.type = type;
        this.openDotBottom = openDotBottom;
        this.closedDotBottom = closedDotBottom;
        //assert lastInLevelMap.length == level;
    }

    public GTreeColumnValue override(GTreeColumnValueType type) {
        return new GTreeColumnValue(level, lastInLevelMap, type, openDotBottom, closedDotBottom);
    }

    public boolean equalsValue(GTreeColumnValue that) {
        if(!(level == that.level &&
                openDotBottom == that.openDotBottom &&
                closedDotBottom == that.closedDotBottom &&
                type == that.type))
            return false;

        for (int i=0; i<lastInLevelMap.length; i++)
            if (lastInLevelMap[i] != that.lastInLevelMap[i])
                return false;

        return true;
    }
}
