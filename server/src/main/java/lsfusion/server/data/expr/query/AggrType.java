package lsfusion.server.data.expr.query;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;

public interface AggrType {

    boolean isSelect(); // оператор выбирает из множества значений (а не суммирует, объединяет и т.п.)
    boolean isSelectNotInWhere(); // в общем то оптимизационная вещь потом можно убрать

    boolean canBeNull(); // может возвращать null если само выражение не null
    
    Where getWhere(ImList<Expr> exprs); // вот тут надо быть аккуратнее, предполагается что первое выражение попадет в getWhere, см. GroupExpr.Query.and
    Expr getMainExpr(ImList<Expr> exprs); // вот тут надо быть аккуратнее, предполагается что первое выражение попадет в getWhere, см. GroupExpr.Query.and
}
