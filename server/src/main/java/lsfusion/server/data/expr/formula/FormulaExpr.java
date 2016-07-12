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
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.*;
import lsfusion.server.data.query.stat.FormulaJoin;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.sql.PostgreDataAdapter;
import lsfusion.server.data.sql.SQLSyntax;
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
//        assert !formula.hasNotNull();
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

    public static String toString(FormulaExprInterface expr) {
        final ImList<BaseExpr> params = expr.getFParams();
        final Where where = expr.getWhere();
        return expr.getFormula().getSource(new ExprSource() {
            public String getSource(int i) {
                return params.get(i).toString();
            }

            public SQLSyntax getSyntax() {
                return PostgreDataAdapter.debugSyntax;
            }

            public MStaticExecuteEnvironment getMEnv() {
                return StaticExecuteEnvironmentImpl.MVOID;
            }

            public int getExprCount() {
                return params.size();
            }

            public Type getType(int i) {
                return params.get(i).getType(where);
            }

            public boolean isToString() {
                return true;
            }
        });
    }
    public static String getSource(FormulaExprInterface expr, final CompileSource compile) {
        if(compile instanceof ToString)
            return toString(expr);

        return expr.getFormula().getSource(new ListExprSource(expr.getFParams()) {
            public CompileSource getCompileSource() {
                return compile;
            }
        });
    }

    public static ConcreteClass getStaticClass(FormulaExprInterface expr) {
        FormulaClass result = getStaticValueClass(expr);
        if(result != null)
            return result;
        return (DataClass)((Expr)expr).getSelfType();
    }

    private static FormulaClass getStaticValueClass(FormulaExprInterface expr) {
        FormulaJoinImpl formula = expr.getFormula();
        if(formula instanceof CustomFormulaImpl && ((CustomFormulaImpl)formula).valueClass instanceof UnknownClass) // так как это один очень частный случай, генерации id'ков
            return ((CustomFormulaImpl)formula).valueClass;
        return null;
    }

    public static AndClassSet getFormulaAndClassSet(FormulaExprInterface expr, final ImMap<VariableSingleClassExpr, AndClassSet> and) {
        FormulaClass staticValueClass = getStaticValueClass(expr);
        if(staticValueClass != null)
            return staticValueClass;
        DataClass result = (DataClass) expr.getFormula().getType(new ListExprType(expr.getFParams()) {
            public Type getType(int i) {
                AndClassSet andClassSet = ((BaseExpr) exprs.get(i)).getAndClassSet(and);
                if(andClassSet != null)
                    return andClassSet.getType();
                return null;
            }
        });
//        ServerLoggers.assertLog(assertStatic(result, getStaticClass(expr)), "");
        return result;
    }
    
    private static boolean assertStatic(DataClass result, ConcreteClass staticClass) {
        if(result != null) {
            return staticClass instanceof DataClass && result.containsAll(staticClass, false);
        }
        return staticClass == null;
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
        return new FormulaJoin<Integer>(expr.getFParams().toIndexedMap(), false);
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
    public String toString() {
        return toString(this);
    }

    public ConcreteClass getStaticClass() {
        return getStaticClass(this);
    }
    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        return getFormulaAndClassSet(this, and);
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
        return createCustomFormula(new CustomFormulaSyntax(formula), valueClass, params, false); 
    }
    
    public static Expr createCustomFormula(final CustomFormulaSyntax formula, final FormulaClass valueClass, ImMap<String, ? extends Expr> params, boolean hasNotNull) {
        ImOrderSet<String> keys = params.keys().toOrderSet();

        CustomFormulaImpl formulaImpl = createCustomFormulaImpl(formula, valueClass, hasNotNull, keys);
        
        ImList<Expr> exprs = keys.mapList(params);
        return create(formulaImpl, exprs);
    }

    public static CustomFormulaImpl createCustomFormulaImpl(CustomFormulaSyntax formula, FormulaClass valueClass, boolean hasNotNull, ImOrderSet<String> keys) {
        ImMap<String, Integer> mapParams = keys.mapOrderValues(new GetIndex<Integer>() {
            @Override
            public Integer getMapValue(int i) {
                return i;
            }
        });
        return new CustomFormulaImpl(formula, mapParams, valueClass, hasNotNull);
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

