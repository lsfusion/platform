package lsfusion.server.data.expr.formula;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.UnknownClass;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.stat.FormulaJoin;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;

public class FormulaExpr extends StaticClassExpr implements FormulaExprInterface {
    public final static String MIN2 = "(prm1+prm2-ABS(prm1-prm2))/2"; // пока так сделаем min и проверку на infinite
    public final static String MULT2 = "(prm1*prm2)";

    protected final ImList<BaseExpr> exprs;

    protected final FormulaJoinImpl formula;

    public ImList<BaseExpr> getFParams() {
        return exprs;
    }

    public FormulaJoinImpl getFormula() {
        return formula;
    }

    private FormulaExpr(ImList<BaseExpr> exprs, FormulaJoinImpl formula) {
        assert !formula.hasNotNull();
        this.exprs = exprs;
        this.formula = formula;
    }

    public static void fillAndJoinWheres(FormulaExprInterface expr, MMap<JoinData, Where> joins, Where andWhere) {
        for (BaseExpr param : expr.getFParams()) {
            param.fillJoinWheres(joins, andWhere);
        }
    }

    public static Expr packFollowFalse(FormulaExprInterface expr, Where where) {
        ImMap<Integer, BaseExpr> indexedExprs = expr.getFParams().toIndexedMap();
        ImMap<Integer, Expr> packParams = packPushFollowFalse(indexedExprs, where);
        if (!BaseUtils.hashEquals(packParams, indexedExprs)) {
            return create(expr.getFormula(), ListFact.fromIndexedMap(packParams));
        }

        return (Expr) expr;
    }

    public static String getSource(FormulaExprInterface expr, final CompileSource compile) {
        return expr.getFormula().getSource(new ListExprSource(expr.getFParams()) {
            public CompileSource getCompileSource() {
                return compile;
            }
        });
    }

    public static ConcreteClass getStaticClass(FormulaExprInterface expr) {
        FormulaJoinImpl formula = expr.getFormula();
        if(formula instanceof CustomFormulaImpl && ((CustomFormulaImpl)formula).valueClass instanceof UnknownClass) // так как это один очень частный случай, генерации id'ков
            return ((CustomFormulaImpl)formula).valueClass;
        return (DataClass)((Expr)expr).getSelfType();
    }

    public static Type getType(FormulaExprInterface expr, final KeyType keyType) {
        return expr.getFormula().getType(new ContextListExprType(expr.getFParams()) {
            public KeyType getKeyType() {
                return keyType;
            }
        });
    }
    
    public static Expr translateQuery(FormulaExprInterface expr, QueryTranslator translator) {
        return create(expr.getFormula(), translator.translate(expr.getFParams()));
    }

    public static Stat getTypeStat(FormulaExprInterface expr, KeyStat keyStat, boolean forJoin) {
        Stat result = Stat.ONE;
        for (BaseExpr param : expr.getFParams()) {
            result = result.mult(param.getTypeStat(keyStat, forJoin));
        }
        return result;
    }
    
    public static InnerBaseJoin<?> getBaseJoin(FormulaExprInterface expr) {
        return new FormulaJoin<Integer>(expr.getFParams().toIndexedMap());
    }

    public static boolean isComplex(FormulaExprInterface expr) {
        return true;
    }

    // множественное наследование

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        fillAndJoinWheres(this, joins, andWhere);
    }

    public Expr packFollowFalse(Where where) {
        return packFollowFalse(this, where);
    }

    public String getSource(final CompileSource compile) {
        return getSource(this, compile);
    }

    public ConcreteClass getStaticClass() {
        return getStaticClass(this);
    }

    public Type getType(final KeyType keyType) {
        return getType(this, keyType);
    }

    public Expr translateQuery(QueryTranslator translator) {
        return translateQuery(this, translator);
    }

    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return getTypeStat(this, keyStat, forJoin);
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return getBaseJoin(this);
    }

    protected boolean isComplex() {
        return isComplex(this);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return exprs.equals(((FormulaExpr) o).exprs) && formula.equals(((FormulaExpr) o).formula);
    }

    protected int hash(HashContext hashContext) {
        return 31 * hashOuter(exprs, hashContext) + formula.hashCode();
    }

    protected FormulaExpr translate(MapTranslate translator) {
        return new FormulaExpr(translator.translateDirect(exprs), formula);
    }

    // для мн-вого наследования
    public static PropStat getStatValue(BaseExpr expr, KeyStat keyStat) {
        return new PropStat(expr.getTypeStat(keyStat, false).min(Stat.AGGR));
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return getStatValue(this, keyStat);
    }

    // методы для создания
    public static Expr createCustomFormula(final String formula, final FormulaClass value, Expr prm1) {
        return createCustomFormula(formula, value, MapFact.singleton("prm1", prm1));
    }

    public static Expr createCustomFormula(final String formula, final FormulaClass value, Expr prm1, Expr prm2) {
        return createCustomFormula(formula, value, MapFact.toMap("prm1", prm1, "prm2", prm2));
    }

    public static Expr createCustomFormula(final String formula, final FormulaClass valueClass, ImMap<String, ? extends Expr> params) {
        return createCustomFormula(formula, valueClass, params, false); 
    }
    
    public static Expr createCustomFormula(final String formula, final FormulaClass valueClass, ImMap<String, ? extends Expr> params, boolean hasNotNull) {
        ImOrderSet<String> keys = params.keys().toOrderSet();

        ImMap<String, Integer> mapParams = keys.mapOrderValues(new GetIndex<Integer>() {
            @Override
            public Integer getMapValue(int i) {
                return i;
            }
        });
        ImList<Expr> exprs = keys.mapList(params);

        return create(new CustomFormulaImpl(formula, mapParams, valueClass, hasNotNull), exprs);
    }

    public static Expr create(final FormulaJoinImpl formula, ImList<? extends Expr> exprs) {
        return new ExprPullWheres<Integer>() {
            protected Expr proceedBase(ImMap<Integer, BaseExpr> map) {
                return createBase(ListFact.fromIndexedMap(map), formula);
            }
        }.proceed(exprs.toIndexedMap());
    }

    private static Expr createBase(ImList<BaseExpr> exprs, final FormulaJoinImpl formula) {
        if (formula instanceof CustomFormulaImpl) {
            CustomFormulaImpl customFormula = (CustomFormulaImpl) formula;
            if (customFormula.formula.equals(MIN2)) {
                assert !formula.hasNotNull();
                BaseExpr operator1 = exprs.get(0);
                BaseExpr operator2 = exprs.get(1);
                if (operator1 instanceof InfiniteExpr) {
                    return operator2;
                }
                if (operator2 instanceof InfiniteExpr) {
                    return operator1;
                }
            }
        }
        return BaseExpr.create(formula.hasNotNull() ? new FormulaNotNullExpr(exprs, formula) : new FormulaExpr(exprs, formula));
    }
}

