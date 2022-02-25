package lsfusion.server.data.expr.formula;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.StaticClassExpr;
import lsfusion.server.data.expr.classes.VariableSingleClassExpr;
import lsfusion.server.data.expr.join.base.FormulaJoin;
import lsfusion.server.data.expr.join.inner.InnerBaseJoin;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.query.exec.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.UnknownClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;

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

    public static void fillAndJoinWheres(FormulaExprInterface expr, MMap<FJData, Where> joins, Where andWhere) {
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
                return DataAdapter.debugSyntax;
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
    public static String getSource(FormulaExprInterface expr, final CompileSource compile, boolean needValue) {
        if(compile instanceof ToString)
            return toString(expr);

        return expr.getFormula().getSource(new ListExprSource(expr.getFParams(), needValue || expr.getFormula().hasNotNull()) {
            public CompileSource getCompileSource() {
                return compile;
            }
        });
    }

    public static ConcreteClass getStaticClass(FormulaExprInterface expr, KeyType keyType) {
        FormulaClass result = getStaticValueClass(expr);
        if(result != null)
            return result;
        Type type = keyType == null ? ((Expr) expr).getSelfType() : ((Expr) expr).getType(keyType);
        return (DataClass) type;
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
    
    public static Expr translateExpr(FormulaExprInterface expr, ExprTranslator translator) {
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
        return new FormulaJoin<>(expr.getFParams().toIndexedMap(), false);
    }

    public static boolean isComplex(FormulaExprInterface expr) {
        return true;
    }

    // множественное наследование

    public void fillAndJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
        fillAndJoinWheres(this, joins, andWhere);
    }

    public Expr packFollowFalse(Where where) {
        return packFollowFalse(this, where);
    }

    public String getSource(final CompileSource compile, boolean needValue) {
        return getSource(this, compile, needValue);
    }
    public String toString() {
        return toString(this);
    }

    @IdentityLazy
    public ConcreteClass getStaticClass() {
        return getStaticClass(null);
    }
    public ConcreteClass getStaticClass(KeyType keyType) {
        return getStaticClass(this, keyType);
    }

    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        return getFormulaAndClassSet(this, and);
    }

    public Type getType(final KeyType keyType) {
        return getType(this, keyType);
    }

    public Expr translate(ExprTranslator translator) {
        return translateExpr(this, translator);
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

    public int hash(HashContext hashContext) {
        return 31 * hashOuter(exprs, hashContext) + formula.hashCode();
    }

    protected FormulaExpr translate(MapTranslate translator) {
        return new FormulaExpr(translator.translateDirect(exprs), formula);
    }

    // для мн-вого наследования
    public static PropStat getStatValue(BaseExpr expr, KeyStat keyStat) {
        return new PropStat(expr.getTypeStat(keyStat, false).min(Stat.AGGR));
    }

    public PropStat getStatValue(KeyStat keyStat, StatType type) {
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
        return createCustomFormula(new CustomFormulaSyntax(formula), false, valueClass, params, false);
    }
    
    public static Expr createCustomFormula(final CustomFormulaSyntax formula, boolean union, final FormulaClass valueClass, ImMap<String, ? extends Expr> params, boolean hasNotNull) {
        ImOrderSet<String> keys = params.keys().toOrderSet();

        ImList<Expr> exprs = keys.mapList(params);

        if(union)
            return FormulaUnionExpr.create(createUnionCustomFormulaImpl(formula, valueClass, keys), exprs);

        return create(createJoinCustomFormulaImpl(formula, valueClass, hasNotNull, keys), exprs);
    }

    public static JoinCustomFormulaImpl createJoinCustomFormulaImpl(CustomFormulaSyntax formula, FormulaClass valueClass, boolean hasNotNull, ImOrderSet<String> keys) {
        ImMap<String, Integer> mapParams = keys.mapOrderValues((int i) -> i);
        return new JoinCustomFormulaImpl(formula, mapParams, valueClass, hasNotNull);
    }

    public static UnionCustomFormulaImpl createUnionCustomFormulaImpl(CustomFormulaSyntax formula, FormulaClass valueClass, ImOrderSet<String> keys) {
        ImMap<String, Integer> mapParams = keys.mapOrderValues((int i) -> i);
        return new UnionCustomFormulaImpl(formula, mapParams, valueClass);
    }

    public static Expr create(final FormulaJoinImpl formula, ImList<? extends Expr> exprs) {
        return new ExprPullWheres<Integer>() {
            protected Expr proceedBase(ImMap<Integer, BaseExpr> map) {
                return createBase(ListFact.fromIndexedMap(map), formula);
            }
        }.proceed(exprs.toIndexedMap());
    }

    private static Expr createBase(ImList<BaseExpr> exprs, final FormulaJoinImpl formula) {
        return BaseExpr.create(formula.hasNotNull() ? new FormulaNullableExpr(exprs, formula) : new FormulaExpr(exprs, formula));
    }
}

