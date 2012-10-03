package platform.server.data.where;

import org.springframework.core.Ordered;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.base.QuickSet;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.innerjoins.*;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Where extends SourceJoin<Where>, OuterContext<Where>, KeyType, KeyStat, CheckWhere {

    Where followFalse(Where falseWhere);
    Where followFalse(Where falseWhere, boolean packExprs);
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

    <K> Map<K, Expr> followTrue(Map<K, ? extends Expr> map, boolean pack);
    List<Expr> followFalse(List<Expr> list, boolean pack);
    <K> OrderedMap<Expr, K> followFalse(OrderedMap<Expr, K> map, boolean pack);

    Where not();

    Where and(Where where);
    Where and(Where where, boolean packExprs);
    Where or(Where where);
    Where or(Where where, boolean packExprs);

    Where xor(Where where);

    OrObjectWhere[] getOr(); // protected

    Map<BaseExpr,BaseExpr> getExprValues();
    Map<BaseExpr,BaseExpr> getNotExprValues();
    Map<BaseExpr,BaseExpr> getOnlyExprValues();

    static String TRUE_STRING = "1=1";
    static String FALSE_STRING = "1<>1";

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    <K extends BaseExpr> Pair<Collection<GroupJoinsWhere>, Boolean> getPackWhereJoins(boolean tryExclusive, QuickSet<K> keepStat, List<Expr> orderTop);
    <K extends BaseExpr> Pair<Collection<GroupJoinsWhere>, Boolean> getWhereJoins(boolean tryExclusive, QuickSet<K> keepStat, List<Expr> orderTop);
    <K extends BaseExpr> Collection<GroupStatWhere<K>> getStatJoins(QuickSet<K> keys, boolean exclusive, GroupStatType type, boolean noWhere);
    <K extends BaseExpr> StatKeys<K> getStatKeys(QuickSet<K> keys);
    <K extends BaseExpr> StatKeys<K> getStatKeys(Collection<K> keys);
    <K extends BaseExpr> Stat getStatRows();
    <K extends Expr> Collection<GroupStatWhere<K>> getStatJoins(boolean notExclusive, QuickSet<K> exprs, GroupStatType type, boolean noWhere);
    <K extends Expr> StatKeys<K> getStatExprs(QuickSet<K> keys);

    // группировки в ДНФ, protected по сути
    KeyEquals getKeyEquals();
    <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(QuickSet<K> keepStat, KeyStat keyStat, List<Expr> orderTop, boolean noWhere);
    MeanClassWheres groupMeanClassWheres(boolean useNots);

    abstract public ClassExprWhere getClassWhere();

    int getHeight();

    static Where TRUE = new AndWhere();
    static Where FALSE = new OrWhere();

    Where translateQuery(QueryTranslator translator);

    Where translateOuter(MapTranslate translator);

    Where map(Map<KeyExpr,? extends Expr> map);

    Where getKeepWhere(KeyExpr expr);

    Where ifElse(Where trueWhere, Where falseWhere);

    boolean isNot(); // assert что not().getNotType()==!getNotType()
}
