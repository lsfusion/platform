package lsfusion.server.data.query.build;

import lsfusion.base.mutability.ImmutableObject;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.CaseJoin;
import lsfusion.server.data.expr.where.ifs.IfJoin;
import lsfusion.server.data.query.translate.MapJoin;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.where.Where;

public abstract class AbstractJoin<U> extends ImmutableObject implements Join<U> {

    public static <U> Join<U> and(Join<U> join, Where where) {
        if(where.isTrue()) // оптимизация
            return join;
        if(Expr.useCasesCount <= 1)
            return new CaseJoin<>(where, join);
        else
            return new IfJoin<>(where, join);
    }

    public Join<U> and(Where where) {
        return and(this, where);
    }

    public static <U> Join<U> translateValues(Join<U> join, MapValuesTranslate translate) {
        return new MapJoin<>(translate, join);
    }
    public Join<U> translateValues(MapValuesTranslate translate) {
        return translateValues(this, translate);
    }
}
