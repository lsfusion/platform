package platform.server.data.query.innerjoins;

import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.query.JoinSet;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.Where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class KeyEquals extends DNFWheres<KeyEqual, Where, KeyEquals> {

    public KeyEquals() {
    }

    public KeyEquals(Where where) {
        super(new KeyEqual(), where);
    }

    public KeyEquals(KeyExpr key, BaseExpr expr) {
        super(new KeyEqual(key, expr), new EqualsWhere(key, expr));
    }

    protected Where andValue(Where prevValue, Where newValue) {
        return prevValue.and(newValue);
    }
    protected Where addValue(Where prevValue, Where newValue) {
        return prevValue.or(newValue);
    }

    protected KeyEquals createThis() {
        return new KeyEquals();
    }

    public Collection<InnerSelectJoin> getInnerJoins(boolean joins) {
        if(!joins && size==1)
            return Collections.singleton(new InnerSelectJoin(getSingleKey(), new JoinSet(), Where.TRUE));           

        Collection<InnerSelectJoin> result = new ArrayList<InnerSelectJoin>();
        for(int i=0;i<size;i++) {
            KeyEqual keyEqual = getKey(i);
            Where where = getValue(i);

            if(joins)
                for(Map.Entry<ObjectJoinSet,Where> objectJoin : where.groupObjectJoinSets().compileMeans().entrySet())
                    result.add(new InnerSelectJoin(keyEqual, objectJoin.getKey().getJoins(), objectJoin.getValue()));
            else
                result.add(new InnerSelectJoin(keyEqual, new JoinSet(), where));
        }
        return result;
    }
}
