package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.expr.join.stat.UnionJoin;
import lsfusion.server.data.where.Where;

// выражение для оптимизации, разворачивание которого в case'ы даст экспоненту
public abstract class UnionExpr extends StaticClassNullableExpr {

    public Where calculateOrWhere() {
        return Expr.getOrWhere(getParams());
    }

    @Override
    public void fillJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
        fillAndJoinWheres(joins, andWhere);
    }

    // мы и так перегрузили fillJoinWheres
    public void fillAndJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
        for(Expr operand : getParams()) // просто гоним по операндам
            operand.fillJoinWheres(joins, andWhere);
    }

    @Override
    protected boolean isComplex() {
        return true;
    }

    @IdentityLazy
    public UnionJoin getBaseJoin() {
        return new UnionJoin(getParams().toSet()); // ??? тут надо было бы getTypeStat использовать, но пока не предполагается использование Linear в Join'ах
    }
}
