package platform.server.where;

import platform.base.QuickSet;
import platform.server.data.Table;
import platform.server.data.query.GroupJoin;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.exprs.GroupExpr;

public class DataWhereSet extends QuickSet<DataWhere> {

    public DataWhereSet() {
    }

    public DataWhereSet(DataWhereSet set) {
        super(set);
    }

    public DataWhereSet(DataWhereSet[] sets) {
        super(sets);
    }

    public boolean contains(InnerJoin join) {
        if(join instanceof Table.Join)
            return contains((DataWhere) ((Table.Join)join).getWhere());
        else
            return contains((GroupJoin) join);
    }

    boolean contains(GroupJoin group) {
        for(int i=0;i<size;i++) {
            DataWhere where = get(i);
            if(where instanceof GroupExpr.NotNull && ((GroupExpr.NotNull)where).getGroupJoin().equals(group))
                return true;
        }
        return false;
    }
}

