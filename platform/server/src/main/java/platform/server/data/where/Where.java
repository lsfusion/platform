package platform.server.data.where;

import platform.base.Pair;
import platform.base.col.interfaces.immutable.*;
import platform.server.caches.OuterContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.innerjoins.*;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;

public interface Where extends SourceJoin<Where>, OuterContext<Where>, KeyType, KeyStat, CheckWhere<Where> {

    Where followFalse(Where falseWhere);
    Where followFalse(Where falseWhere, boolean packExprs);
    Where followFalseChange(Where falseWhere, boolean packExprs, FollowChange change);

    Where followFalse(CheckWhere falseWhere, boolean pack, FollowChange change); // protected

    static enum FollowType { // protected
        WIDE,NARROW,DIFF,EQUALS;

        public FollowType or(FollowType or) {
            if(this==FollowType.DIFF || this==or || or==FollowType.EQUALS)
                return this;
            if(or==FollowType.DIFF || this==FollowType.EQUALS)
                return or;
            return FollowType.DIFF;
        }
    }

    static class FollowChange { // protected

        public FollowType type = FollowType.EQUALS;
        void not() {
            switch(type) {
                case WIDE:
                    type=FollowType.NARROW;
                    break;
                case NARROW:
                    type=FollowType.WIDE;
                    break;
            }
        }
    }

    <K> ImMap<K, Expr> followTrue(ImMap<K, ? extends Expr> map, boolean pack);
    ImList<Expr> followFalse(ImList<Expr> list, boolean pack);
    ImSet<Expr> followFalse(ImSet<Expr> set, boolean pack);
    <K> ImOrderMap<Expr, K> followFalse(ImOrderMap<Expr, K> map, boolean pack);

    Where not();

    Where and(Where where);
    Where and(Where where, boolean packExprs);
    Where or(Where where);
    Where or(Where where, boolean packExprs);

    Where exclOr(Where where); // вообще толку с него мало, собсно чтобы проверить в профайлере надо ли оптимизировать

    Where xor(Where where);

    OrObjectWhere[] getOr(); // protected

    ImMap<BaseExpr, BaseExpr> getExprValues();
    ImMap<BaseExpr, BaseExpr> getNotExprValues();
    ImMap<BaseExpr, BaseExpr> getOnlyExprValues();

    static String TRUE_STRING = "1=1";
    static String FALSE_STRING = "1<>1";

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    <K extends BaseExpr> Pair<ImCol<GroupJoinsWhere>, Boolean> getPackWhereJoins(boolean tryExclusive, ImSet<K> keepStat, ImOrderSet<Expr> orderTop);
    <K extends BaseExpr> Pair<ImCol<GroupJoinsWhere>, Boolean> getWhereJoins(boolean tryExclusive, ImSet<K> keepStat, ImOrderSet<Expr> orderTop);
    <K extends BaseExpr> ImCol<GroupStatWhere<K>> getStatJoins(ImSet<K> keys, boolean exclusive, GroupStatType type, boolean noWhere);
    <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> keys);
    <K extends BaseExpr> Stat getStatRows();
    <K extends Expr> ImCol<GroupStatWhere<K>> getStatJoins(boolean notExclusive, ImSet<K> exprs, GroupStatType type, boolean noWhere);
    <K extends Expr> StatKeys<K> getStatExprs(ImSet<K> keys);

    // группировки в ДНФ, protected по сути
    KeyEquals getKeyEquals();
    <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, boolean noWhere);
    MeanClassWheres groupMeanClassWheres(boolean useNots);

    abstract public ClassExprWhere getClassWhere();

    int getHeight();

    static Where TRUE = new AndWhere();
    static Where FALSE = new OrWhere();

    Where translateQuery(QueryTranslator translator);

    Where translateOuter(MapTranslate translator);

    Where map(ImMap<KeyExpr, ? extends Expr> map);

    Where getKeepWhere(KeyExpr expr);

    Where ifElse(Where trueWhere, Where falseWhere);

    boolean isNot(); // assert что not().getNotType()==!getNotType()
}
