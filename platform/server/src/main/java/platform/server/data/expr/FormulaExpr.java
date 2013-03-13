package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.DataClass;
import platform.server.data.expr.query.PropStat;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.stat.FormulaJoin;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.Iterator;

import static platform.base.BaseUtils.nullEquals;
import static platform.base.BaseUtils.nullHash;

public class FormulaExpr extends StaticClassExpr {

    public final static String MIN2 = "(prm1+prm2-ABS(prm1-prm2))/2"; // пока так сделаем min и проверку на infinite
    public final static String MULT2 = "(prm1*prm2)";

    private final String formula;
    private final ConcreteClass valueClass;
    private final ImMap<String, BaseExpr> params;

    // этот конструктор напрямую можно использовать только заведомо зная что getClassWhere не null или через оболочку create 
    private FormulaExpr(String formula,ImMap<String, BaseExpr> params, ConcreteClass valueClass) {
        this.formula = formula;
        this.params = params;
        this.valueClass = valueClass;
    }

    public static Expr create(String formula, ImMap<String, BaseExpr> params, ConcreteClass value) {
        if(formula.equals(MIN2)) {
            Iterator<BaseExpr> i = params.values().iterator();
            BaseExpr operator1 = i.next(); BaseExpr operator2 = i.next();
            if(operator1 instanceof InfiniteExpr)
                return operator2;
            if(operator2 instanceof InfiniteExpr)
                return operator1;
        }

        return BaseExpr.create(new FormulaExpr(formula, params, value));
    }

    public static Expr create1(final String formula, final ConcreteClass value,Expr prm1) {
        return create(formula, value, MapFact.singleton("prm1", prm1));
    }
    public static Expr create2(final String formula, final ConcreteClass value,Expr prm1, Expr prm2) {
        return create(formula, value, MapFact.toMap("prm1", prm1, "prm2", prm2));
    }

    public static Expr create(final String formula, final ConcreteClass value,ImMap<String,? extends Expr> params) {
        return new ExprPullWheres<String>() {
            protected Expr proceedBase(ImMap<String, BaseExpr> map) {
                return create(formula, map, value);
            }
        }.proceed(params);
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        for(BaseExpr param : params.values())
            param.fillJoinWheres(joins, andWhere);
    }

    public static String getSource(String formula, ImMap<String, ? extends Expr> params, Type type, CompileSource compile) {
        String sourceString = formula;
        for(int i=0,size=params.size();i<size;i++)
            sourceString = sourceString.replace(params.getKey(i), params.getValue(i).getSource(compile));
         return "("+sourceString+")"; // type.getCast(sourceString, compile.syntax, false)
    }

    public String getSource(CompileSource compile) {
        return getSource(formula, params, getSelfType(), compile);
     }

    public Type getType(KeyType keyType) {
        ConcreteClass staticClass = getStaticClass();
        if(staticClass==null)
            return null;
        return staticClass.getType();
    }
    public Stat getTypeStat(KeyStat keyStat) {
        return getStaticClass().getTypeStat();
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return create(formula, valueClass, translator.translate(params));
    }

    protected BaseExpr translate(MapTranslate translator) {
        return new FormulaExpr(formula,translator.translateDirect(params),valueClass);
    }

    @Override
    public Expr packFollowFalse(Where where) {
        ImMap<String, Expr> packParams = packPushFollowFalse(params, where);
        if(!BaseUtils.hashEquals(packParams, params)) 
            return create(formula, valueClass, packParams);
        else
            return this;
    }

    public boolean twins(TwinImmutableObject o) {
        return formula.equals(((FormulaExpr) o).formula) && params.equals(((FormulaExpr) o).params) && nullEquals(valueClass, ((FormulaExpr) o).valueClass);
    }

    protected boolean isComplex() {
        return true;
    }
    protected int hash(HashContext hashContext) {
        return (nullHash(valueClass) * 31 * 31) + (hashOuter(params, hashContext) * 31) + formula.hashCode();
    }

    public static Type getCompatibleType(ImCol<? extends Expr> exprs) {
        assert exprs.size()>0;

        DataClass type = null;
        for(Expr expr : exprs)
            if(!(expr instanceof KeyExpr)) {
                DataClass exprType = (DataClass) expr.getSelfType();
                if(type==null)
                    type = exprType;
                else
                    type = type.getCompatible(exprType);
            }
        return type;
    }

    @IdentityLazy
    private DataClass getCompatibleClass() {
        return (DataClass) getCompatibleType(params.values());
    }

    public ConcreteClass getStaticClass() {
        if(valueClass==null)
            return getCompatibleClass();
        else
            return valueClass;
    }

    // для мн-вого наследования
    public static PropStat getStatValue(BaseExpr expr, KeyStat keyStat) {
        return new PropStat(expr.getTypeStat(keyStat));
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return getStatValue(this, keyStat);
    }
    public InnerBaseJoin<?> getBaseJoin() {
        return new FormulaJoin<String>(params);
    }
}

