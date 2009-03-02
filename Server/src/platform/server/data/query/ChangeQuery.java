package platform.server.data.query;

import platform.server.data.query.exprs.CaseExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.JoinWhere;

import java.util.Collection;

// выбирает по списку значение из первого Source'а
public class ChangeQuery<K,V> extends UnionQuery<K,V> {

    public ChangeQuery(Collection<? extends K> iKeys) {
        super(iKeys);
    }

    SourceExpr getUnionExpr(SourceExpr prevExpr, SourceExpr expr, JoinWhere inJoin) {
        return new CaseExpr(inJoin,expr,prevExpr);
    }
}
