package lsfusion.server.logics.form.stat;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

import static lsfusion.base.BaseUtils.nvl;

public class SelectTop {
    public static SelectTop NULL = new SelectTop(0, null);

    private final Integer selectTop;
    private final ImOrderMap<GroupObjectEntity, Integer> selectTops;

    public SelectTop(Integer selectTop) {
        this(selectTop, null);
    }

    public SelectTop(Integer selectTop, ImOrderMap<GroupObjectEntity, Integer> selectTops) {
        this.selectTop = selectTop;
        this.selectTops = selectTops;
    }

    public int get(GroupObjectEntity group) {
        return nvl(selectTops != null && group != null ? nvl(selectTops.get(group), selectTop) : selectTop, 0);
    }
}
