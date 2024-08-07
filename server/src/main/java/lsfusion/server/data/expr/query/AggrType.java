package lsfusion.server.data.expr.query;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;

public interface AggrType {

    // the operator selects from a set of values (rather than summarizing, combining, etc.)
    default boolean isSelect() {
        return false;
    }
    // generally an optimization thing you can remove afterwards
    default boolean isSelectNotInWhere() {
        assert isSelect();
        return false;
    }

    default Type getType(Type exprType) {
        return exprType;
    }
    default Stat getTypeStat(Stat typeStat, boolean forJoin) {
        return typeStat;
    }
    default Type getType(ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, Where where) {
        return getType(getMainExpr(exprs, orders).getType(where));
    }
    default Stat getTypeStat(boolean forJoin, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, Where where) {
        return getTypeStat(getMainExpr(exprs, orders).getTypeStat(where, forJoin), forJoin);
    }
    default int getMainIndex(int props) {
        return 0;
    }

    default <X> X getMain(ImList<X> props, ImOrderMap<X, Boolean> orders) {
        int mainIndex = getMainIndex(props.size());

        if(mainIndex < props.size())
            return props.get(mainIndex);

        return orders.getKey(mainIndex - props.size());
    }

    // may return null if the expression itself is not null
    default boolean canBeNull() {
        return false;
    }

    // there is an assertion that first expr is in where, see (PartitionExpr / GroupExpr).Query.and
    default Where getWhere(ImList<Expr> exprs) {
        return Expr.getWhere(exprs);
    }
    // used to get the result type + last opt, there is an assertion that first expr is in where, see (PartitionExpr / GroupExpr).Query.and
    default Expr getMainExpr(ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders) {
        return getMain(exprs, orders);
    }
    default ImList<Expr> followFalse(Where falseWhere, ImList<Expr> exprs, boolean pack) {
        return falseWhere.followFalse(exprs, pack);
    }
}
