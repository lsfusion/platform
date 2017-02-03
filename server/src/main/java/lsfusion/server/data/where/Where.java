package lsfusion.server.data.where;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.expr.query.GroupExprWhereJoins;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.SourceJoin;
import lsfusion.server.data.query.innerjoins.*;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.ExprTranslator;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.data.where.classes.MeanClassWheres;

public interface Where extends SourceJoin<Where>, OuterContext<Where>, KeyType, KeyStat, CheckWhere<Where> {

    Where followFalse(Where falseWhere);
    Where followFalse(Where falseWhere, boolean packExprs);
    Where followFalseChange(Where falseWhere, boolean packExprs, FollowChange change);

    Where followFalse(CheckWhere falseWhere, boolean pack, FollowChange change); // protected

    enum FollowType { // protected
        WIDE,NARROW,DIFF,EQUALS;

        public FollowType or(FollowType or) {
            if(this==FollowType.DIFF || this==or || or==FollowType.EQUALS)
                return this;
            if(or==FollowType.DIFF || this==FollowType.EQUALS)
                return or;
            return FollowType.DIFF;
        }
    }

    class FollowChange { // protected

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

    String TRUE_STRING = "1=1";
    String FALSE_STRING = "1<>1";

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    <K extends BaseExpr> Pair<ImCol<GroupJoinsWhere>, Boolean> getPackWhereJoins(boolean tryExclusive, ImSet<K> keepStat, ImOrderSet<Expr> orderTop);
    <K extends BaseExpr> Pair<ImCol<GroupJoinsWhere>, Boolean> getWhereJoins(boolean tryExclusive, ImSet<K> keepStat, StatType statType, ImOrderSet<Expr> orderTop);
    <K extends BaseExpr> ImCol<GroupSplitWhere<K>> getSplitJoins(ImSet<K> keys, StatType statType, boolean exclusive, GroupStatType type);
    <K extends BaseExpr> ImCol<GroupJoinsWhere> getWhereJoins(ImSet<K> keys, StatType statType);
    <K extends BaseExpr> StatKeys<K> getPushedStatKeys(ImSet<K> keys, StatType type, StatKeys<KeyExpr> pushedKeys);
    <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> keys, StatType type);
    <K extends ParamExpr> StatKeys<K> getFullStatKeys(ImSet<K> groups, StatType type);
    <K extends BaseExpr> Stat getStatRows(StatType type);
    <K extends Expr> ImCol<GroupSplitWhere<K>> getSplitJoins(boolean notExclusive, ImSet<K> exprs, StatType statType, GroupStatType type);
    <K extends Expr> GroupExprWhereJoins<K> getGroupWhereJoins(ImSet<K> exprs, final StatType statType);

    // группировки в ДНФ, protected по сути
    KeyEquals getKeyEquals();
    <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, StatType statType, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type);
    MeanClassWheres groupMeanClassWheres(boolean useNots);

    ClassExprWhere getClassWhere();

    int getHeight();

    Where TRUE = new AndWhere();
    Where FALSE = new OrWhere();

    Where translateExpr(ExprTranslator translator);

    Where translateOuter(MapTranslate translator);

    Where mapWhere(ImMap<KeyExpr, ? extends Expr> map);

    Where getKeepWhere(KeyExpr expr);

    Where ifElse(Where trueWhere, Where falseWhere);

    boolean isNot(); // assert что not().getNotType()==!getNotType()
}
