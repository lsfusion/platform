package platform.server.data.expr.formula;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.*;
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

public class FormulaExpr extends StaticClassExpr {
    public final static String MIN2 = "(prm1+prm2-ABS(prm1-prm2))/2"; // пока так сделаем min и проверку на infinite
    public final static String MULT2 = "(prm1*prm2)";
    protected final ImList<BaseExpr> exprs;

    protected final FormulaImpl formula;

    protected final ExprSource exprSource;

    private FormulaExpr(ImList<BaseExpr> exprs, FormulaImpl formula) {
        this.exprs = exprs;
        this.formula = formula;
        this.exprSource = new ListExprSource(exprs);
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        for (BaseExpr param : exprs) {
            param.fillJoinWheres(joins, andWhere);
        }
    }

    @Override
    public Expr packFollowFalse(Where where) {
        ImMap<Integer, BaseExpr> indexedExprs = exprs.toIndexedMap();
        ImMap<Integer, Expr> packParams = packPushFollowFalse(indexedExprs, where);
        if (!BaseUtils.hashEquals(packParams, indexedExprs)) {
            return create(ListFact.fromIndexedMap(packParams), formula);
        }

        return this;
    }

    @Override
    public String getSource(CompileSource compile) {
        return formula.getSource(compile, exprSource);
    }

    public ConcreteClass getStaticClass() {
        return formula.getStaticClass(exprSource);
    }

    public Type getType(KeyType keyType) {
        return formula.getType(exprSource, keyType);
    }

    public boolean twins(TwinImmutableObject o) {
        return exprs.equals(((FormulaExpr) o).exprs) && formula.equals(((FormulaExpr) o).formula);
    }

    protected int hash(HashContext hashContext) {
        return 31 * hashOuter(exprs, hashContext) + formula.hashCode();
    }

    protected FormulaExpr translate(MapTranslate translator) {
        return new FormulaExpr(translator.translateDirect(exprs), formula);
    }

    @Override
    public Expr translateQuery(QueryTranslator translator) {
        return create(translator.translate(exprs), formula);
    }

    public Stat getTypeStat(KeyStat keyStat) {
        Stat result = Stat.ONE;
        for (BaseExpr expr : exprs) {
            result = result.mult(expr.getTypeStat(keyStat));
        }
        return result;
    }

    protected boolean isComplex() {
        return true;
    }

    // для мн-вого наследования
    public static PropStat getStatValue(BaseExpr expr, KeyStat keyStat) {
        return new PropStat(expr.getTypeStat(keyStat));
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return getStatValue(this, keyStat);
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return new FormulaJoin<Integer>(exprs.toIndexedMap());
    }

    // методы для создания
    public static Expr createCustomFormula(final String formula, final ConcreteClass value, Expr prm1) {
        return createCustomFormula(formula, value, MapFact.singleton("prm1", prm1));
    }

    public static Expr createCustomFormula(final String formula, final ConcreteClass value, Expr prm1, Expr prm2) {
        return createCustomFormula(formula, value, MapFact.toMap("prm1", prm1, "prm2", prm2));
    }

    public static Expr createCustomFormula(final String formula, final ConcreteClass valueClass, ImMap<String, ? extends Expr> params) {
        ImOrderSet<String> keys = params.keys().toOrderSet();

        ImMap<String, Integer> mapParams = keys.mapOrderValues(new GetIndex<Integer>() {
            @Override
            public Integer getMapValue(int i) {
                return i;
            }
        });
        ImList<Expr> exprs = keys.map(params);

        return create(exprs, new CustomFormulaImpl(formula, mapParams, valueClass));
    }

    public static Expr create(ImList<? extends Expr> exprs, final FormulaImpl formula) {
        return new ExprPullWheres<Integer>() {
            protected Expr proceedBase(ImMap<Integer, BaseExpr> map) {
                return createBase(ListFact.fromIndexedMap(map), formula);
            }
        }.proceed(exprs.toIndexedMap());
    }

    private static Expr createBase(ImList<BaseExpr> exprs, final FormulaImpl formula) {
        if (formula instanceof CustomFormulaImpl) {
            CustomFormulaImpl customFormula = (CustomFormulaImpl) formula;
            if (customFormula.formula.equals(MIN2)) {
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
        return BaseExpr.create(new FormulaExpr(exprs, formula));
    }
}

