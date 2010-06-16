package platform.server.data.where;

import platform.base.QuickSet;
import platform.server.data.expr.VariableExprSet;

public class DataWhereSet extends QuickSet<DataWhere> {

    public DataWhereSet() {
    }

    public DataWhereSet(DataWhereSet set) {
        super(set);
    }

    public DataWhereSet(VariableExprSet set) {
        for(int i=0;i<set.size;i++)
            set.get(i).fillFollowSet(this);
    }
}

