package lsfusion.server.data.query.innerjoins;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.TranslateContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.where.extra.EqualsWhere;
import lsfusion.server.data.query.ExprEqualsJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.query.stat.WhereJoins;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.PartialQueryTranslator;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.Where;

public class KeyEqual extends TwinImmutableObject implements DNFWheres.Interface<KeyEqual>,TranslateContext<KeyEqual> {

    public final ImMap<ParamExpr, BaseExpr> keyExprs;

    private KeyEqual() {
        this.keyExprs = MapFact.EMPTY();
    }
    public final static KeyEqual EMPTY = new KeyEqual();

    public KeyEqual(ParamExpr key, BaseExpr expr) {
        keyExprs = MapFact.singleton(key, expr);
    }

    public KeyEqual(ImMap<ParamExpr, BaseExpr> keyExprs) {
        this.keyExprs = keyExprs;
    }
    
    private final static AddValue<ParamExpr, Expr> keepValue = new SymmAddValue<ParamExpr, Expr>() {
        public Expr addValue(ParamExpr key, Expr prevValue, Expr newValue) {
            if(!prevValue.isValue()) // если было не value, предпочтительнее использовать value;
                return newValue;
            return prevValue;
        }
    };
    public static <E extends Expr> AddValue<ParamExpr, E> keepValue() {
        return (AddValue<ParamExpr, E>) keepValue;
    }

    public KeyEqual and(KeyEqual and) {
        return new KeyEqual(keyExprs.merge(and.keyExprs, KeyEqual.<BaseExpr>keepValue()));
    }

    public boolean isFalse() {
        return false;
    }

    public boolean isEmpty() {
        return keyExprs.isEmpty();
    }

    @IdentityInstanceLazy
    public QueryTranslator getTranslator() {
        return new PartialQueryTranslator(keyExprs);
    }

    public Where getWhere() {
        Where equalsWhere = Where.TRUE;
        for(int i=0,size=keyExprs.size();i<size;i++)
            equalsWhere = equalsWhere.and(EqualsWhere.create(keyExprs.getKey(i),keyExprs.getValue(i)));
        return equalsWhere;
    }

    public boolean twins(TwinImmutableObject o) {
        return keyExprs.equals(((KeyEqual) o).keyExprs);
    }

    public int immutableHashCode() {
        return keyExprs.hashCode();
    }

    public static KeyEqual getKeyEqual(BaseExpr operator1, BaseExpr operator2) {
        if(operator1 instanceof ParamExpr && !operator2.hasKey((ParamExpr) operator1))
            return new KeyEqual((ParamExpr) operator1, operator2);
        if(operator2 instanceof ParamExpr && !operator1.hasKey((ParamExpr) operator2))
            return new KeyEqual((ParamExpr) operator2, operator1);
        return KeyEqual.EMPTY;
    }

    public WhereJoins getWhereJoins() {
        WhereJoin[] wheres = new WhereJoin[keyExprs.size()]; int iw = 0;
        for(int i=0,size=keyExprs.size();i<size;i++)
            wheres[iw++] = new ExprEqualsJoin(keyExprs.getKey(i), keyExprs.getValue(i));
        return new WhereJoins(wheres);
    }
    
    public KeyStat getKeyStat(final KeyStat keyStat) {
        return new lsfusion.server.data.query.stat.KeyStat() {
            public Stat getKeyStat(ParamExpr key, boolean forJoin) {
                BaseExpr keyExpr = keyExprs.get(key);
                if(keyExpr!=null)
                    return keyExpr.getTypeStat(keyStat, forJoin);
                else
                    return keyStat.getKeyStat(key, forJoin);
            }
        };
    }

    public KeyEqual translateOuter(MapTranslate translate) {
        return new KeyEqual(translate.translateMap(keyExprs));
    }
}
