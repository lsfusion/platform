package platform.server.data.query;

import platform.base.AddSet;
import platform.base.BaseUtils;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.query.stat.WhereJoins;
import platform.server.data.where.Where;

import java.util.Iterator;
import java.util.Map;

public class InnerJoins extends AddSet<InnerJoin, InnerJoins> implements Iterable<InnerJoin> {

    @Override
    public Iterator<InnerJoin> iterator() {
        return new Iterator<InnerJoin>() {
            int i=0;
            public boolean hasNext() {
                return i<wheres.length;
            }
            public InnerJoin next() {
                return wheres[i++];
            }
            public void remove() {
                throw new RuntimeException("not supported");
            }
        };
    }

    public InnerJoins() {
    }

    public InnerJoins(InnerJoin where) {
        super(where);
    }

    public InnerJoins(InnerJoin[] wheres) {
        super(wheres);
    }

    protected InnerJoins createThis(InnerJoin[] wheres) {
        return new InnerJoins(wheres);
    }

    protected InnerJoin[] newArray(int size) {
        return new InnerJoin[size];
    }

    protected boolean containsAll(InnerJoin who, InnerJoin what) {
        return BaseUtils.hashEquals(who, what) || what.getInnerExpr(who)!=null;
    }

    public boolean means(InnerJoin inner) {
        for(InnerJoin where : wheres)
            if(containsAll(where, inner))
                return true;
        return false;
    }

    public InnerJoins and(InnerJoins joins) {
        return add(joins);
    }

    public Map<InnerJoin, Where> andUpWheres(Map<InnerJoin, Where> up1, Map<InnerJoin, Where> up2) {
        return WhereJoins.andUpWheres(wheres, up1, up2);
    }
}
