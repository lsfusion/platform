package platform.server.data.expr.query;

import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

import java.util.List;

public interface AggrType {

    boolean isSelect(); // оператор выбирает из множества значений (а не суммирует, объединяет и т.п.)
    boolean isSelectNotInWhere(); // в общем то оптимизационная вещь потом можно убрать

    boolean canBeNull(); // может возвращать null если само выражение не null
    
    Where getWhere(List<Expr> exprs); // вот тут надо быть аккуратнее, предполагается что первое выражение попадет в getWhere, см. GroupExpr.Query.and
    Expr getMainExpr(List<Expr> exprs); // вот тут надо быть аккуратнее, предполагается что первое выражение попадет в getWhere, см. GroupExpr.Query.and
}
