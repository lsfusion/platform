package platform.server.data.where;

import platform.base.QuickSet;
import platform.server.data.expr.InnerExprSet;

public class DataWhereSet extends QuickSet<DataWhere> {

    public DataWhereSet() {
    }

    public DataWhereSet(DataWhereSet set) {
        super(set);
    }

    public DataWhereSet(InnerExprSet set) {
        for(int i=0;i<set.size;i++)
            set.get(i).fillFollowSet(this);
    }
}

