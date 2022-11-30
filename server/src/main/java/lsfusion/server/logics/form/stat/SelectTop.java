package lsfusion.server.logics.form.stat;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

import static lsfusion.base.BaseUtils.nvl;

public class SelectTop {
    public static SelectTop NULL = new SelectTop(0, null);

    Integer selectTop;
    OrderedMap<GroupObjectEntity, Integer> selectTops;

    public SelectTop(Integer selectTop) {
        this(selectTop, null);
    }

    public SelectTop(Integer selectTop, OrderedMap<GroupObjectEntity, Integer> selectTops) {
        this.selectTop = selectTop;
        this.selectTops = selectTops;
    }

    public int get(GroupObjectEntity group) {
        return nvl(selectTops != null ? selectTops.getOrDefault(group, selectTop) : selectTop, 0);
    }
}
