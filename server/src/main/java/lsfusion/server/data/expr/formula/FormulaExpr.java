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

public class FormulaExpr extends StaticClassExpr {
    public final static String MIN2 = "(prm1+prm2-ABS(prm1-prm2))/2"; // пока так сделаем min и проверку на infinite
    public final static String MULT2 = "(prm1*prm2)";
    protected final ImList<BaseExpr> exprs;

    protected final FormulaJoinImpl formula;

    private FormulaExpr(ImList<BaseExpr> exprs, FormulaJoinImpl formula) {
        this.exprs = exprs;
        this.formula = formula;
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
            return create(formula, ListFact.fromIndexedMap(packParams));
        }

        return this;
    }

    @Override
    public String getSource(final CompileSource compile) {
        return formula.getSource(new ListExprSource(exprs) {
            public CompileSource getCompileSource() {
                return compile;
            }});
    }

    public ConcreteClass getStaticClass() {
        if(formula instanceof CustomFormulaImpl && ((CustomFormulaImpl)formula).valueClass instanceof UnknownClass) // так как это один очень частный случай, генерации id'ков
            return ((CustomFormulaImpl)formula).valueClass;
        return (DataClass)getSelfType();
    }

    public Type getType(final KeyType keyType) {
        return formula.getType(new ContextListExprType(exprs) {
            public KeyType getKeyType() {
                return keyType;
            }});
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
        return create(formula, translator.translate(exprs));
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
    public static Expr createCustomFormula(final String formula, final FormulaClass value, Expr prm1) {
        return createCustomFormula(formula, value, MapFact.singleton("prm1", prm1));
    }

    public static Expr createCustomFormula(final String formula, final FormulaClass value, Expr prm1, Expr prm2) {
        return createCustomFormula(formula, value, MapFact.toMap("prm1", prm1, "prm2", prm2));
    }

    public static Expr createCustomFormula(final String formula, final FormulaClass valueClass, ImMap<String, ? extends Expr> params) {
        ImOrderSet<String> keys = params.keys().toOrderSet();

        ImMap<String, Integer> mapParams = keys.mapOrderValues(new GetIndex<Integer>() {
            @Override
            public Integer getMapValue(int i) {
                return i;
            }
        });
        ImList<Expr> exprs = keys.mapList(params);

        return create(new CustomFormulaImpl(formula, mapParams, valueClass), exprs);
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

