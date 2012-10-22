package platform.server.data.query;

import platform.base.AddSet;
import platform.base.BaseUtils;
import platform.base.Result;
import platform.server.data.expr.query.QueryJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.query.stat.WhereJoins;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class InnerJoins extends AddSet<InnerJoin, InnerJoins> {

    public InnerJoins() {
    }

    public InnerJoins(InnerJoin where) {
        super(where);
    }

    public InnerJoins(InnerJoin[] wheres) {
        super(wheres);
    }

    public InnerJoins(Collection<InnerJoin> wheres) {
        super(wheres.toArray(new InnerJoin[wheres.size()]));
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

    public WhereJoins removeJoin(QueryJoin removeJoin, Map<WhereJoin, Where> upWheres, Result<Map<WhereJoin, Where>> resultWheres) {
        return WhereJoins.removeJoin(removeJoin, wheres, upWheres, resultWheres);
    }

    // транслятор используется только для InnerJoins без ключей
    public InnerJoins translate(MapValuesTranslate translate) {
        MapTranslate mapKeys = translate.mapKeys();
        InnerJoin[] transWheres = new InnerJoin[wheres.length];
        for(int i=0;i<wheres.length;i++)
            transWheres[i] = ((InnerJoin<?, ?>)wheres[i]).translateOuter(mapKeys);
        return new InnerJoins(transWheres);
    }
}
