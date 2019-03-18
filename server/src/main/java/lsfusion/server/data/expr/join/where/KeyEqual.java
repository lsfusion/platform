package lsfusion.server.data.expr.join.where;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.select.ExprEqualsJoin;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.where.classes.data.EqualsWhere;
import lsfusion.server.data.query.compile.where.DataUpWhere;
import lsfusion.server.data.query.compile.where.UpWhere;
import lsfusion.server.data.query.compile.where.UpWheres;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.PartialKeyExprTranslator;
import lsfusion.server.data.translate.TranslateContext;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.Where;

public class KeyEqual extends AbstractOuterContext<KeyEqual> implements DNFWheres.Interface<KeyEqual>, TranslateContext<KeyEqual> {

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

    public KeyEqual or(KeyEqual and) {
        return new KeyEqual(keyExprs.mergeEquals(and.keyExprs));
    }

    public boolean isFalse() {
        return false;
    }

    public boolean isEmpty() {
        return keyExprs.isEmpty();
    }

    @IdentityInstanceLazy
    public ExprTranslator getTranslator() {
        return new PartialKeyExprTranslator(keyExprs);
    }

    public Where getWhere() {
        Where equalsWhere = Where.TRUE;
        for(int i=0,size=keyExprs.size();i<size;i++)
            equalsWhere = equalsWhere.and(EqualsWhere.create(keyExprs.getKey(i),keyExprs.getValue(i)));
        return equalsWhere;
    }

    public boolean calcTwins(TwinImmutableObject o) {
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
        return getWhereJoins(null);
    }
    public WhereJoins getWhereJoins(Result<UpWheres<WhereJoin>> upWheres) {
        WhereJoin[] wheres = new WhereJoin[keyExprs.size()]; int iw = 0;
        MExclMap<WhereJoin, UpWhere> mUpWheres = null;
        if(upWheres != null)
            mUpWheres = MapFact.mExclMap(keyExprs.size());
        for(int i=0,size=keyExprs.size();i<size;i++) {
            ExprEqualsJoin join = new ExprEqualsJoin(keyExprs.getKey(i), keyExprs.getValue(i));
            wheres[iw++] = join;
            if(mUpWheres != null)
                mUpWheres.exclAdd(join, new DataUpWhere(new EqualsWhere(keyExprs.getKey(i),keyExprs.getValue(i)))); // orWhere не надо, так как они подтягиваются в WhereJoins.getUpWhere (да и вообще KeyEqual создает EqualsWhere создает , а его базовое поведение создавать UpWhere без or'ов) 
        }
        if(mUpWheres != null)
            upWheres.set(new UpWheres<>(mUpWheres.immutable()));
        return new WhereJoins(wheres);
    }
    
    public KeyStat getKeyStat(final KeyStat keyStat) {
        return new KeyStat() {
            public Stat getKeyStat(ParamExpr key, boolean forJoin) {
                BaseExpr keyExpr = keyExprs.get(key);
                if(keyExpr!=null)
                    return keyExpr.getTypeStat(keyStat, forJoin);
                else
                    return keyStat.getKeyStat(key, forJoin);
            }
        };
    }

    protected ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.mergeSet(keyExprs.keys(), BaseUtils.<ImSet<OuterContext>>immutableCast(keyExprs.values().toSet()));
    }

    protected KeyEqual translate(MapTranslate translator) {
        return new KeyEqual(translator.translateMap(keyExprs));
    }

    public int hash(HashContext hash) {
        return AbstractOuterContext.hashMapOuter(keyExprs, hash);
    }
}
