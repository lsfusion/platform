package lsfusion.server.data.query;

import lsfusion.base.AddSet;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.query.QueryJoin;
import lsfusion.server.data.query.innerjoins.UpWheres;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.query.stat.WhereJoins;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.where.Where;

public class InnerJoins extends AddSet<InnerJoin, InnerJoins> {

    private InnerJoins() {
    }

    public static InnerJoins EMPTY = new InnerJoins();

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

    public InnerJoins and(InnerJoins joins) {
        return add(joins);
    }

    public UpWheres<InnerJoin> andUpWheres(UpWheres<InnerJoin> up1, UpWheres<InnerJoin> up2) {
        return WhereJoins.andUpWheres(wheres, up1, up2);
    }

    public WhereJoins removeJoin(QueryJoin removeJoin, UpWheres<WhereJoin> upWheres, Result<UpWheres<WhereJoin>> resultWheres) {
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
