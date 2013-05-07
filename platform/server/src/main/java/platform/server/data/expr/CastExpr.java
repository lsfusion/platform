package platform.server.data.expr;

import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.DataClass;
import platform.server.data.expr.formula.FormulaExpr;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CastExpr extends StaticClassExpr {

    private final BaseExpr expr;

    private final Type castType;

    // этот конструктор напрямую можно использовать только заведомо зная что getClassWhere не null или через оболочку create
    private CastExpr(BaseExpr expr, Type castType) {
        assert castType instanceof DataClass;
        this.expr = expr;
        this.castType = castType;
    }

    public static Expr create(BaseExpr expr, Type castType) {
        return BaseExpr.create(new CastExpr(expr, castType));
    }

    public static Expr create(final Expr expr, final Type castType) {
        return new ExprPullWheres<String>() {
            protected Expr proceedBase(ImMap<String, BaseExpr> map) {
                return create(map.get("cast"), castType);
            }
        }.proceed(MapFact.singleton("cast", expr));
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        expr.fillJoinWheres(joins, andWhere);
    }

    public static <T extends Expr> String getSource(String formula, Pattern paramsPattern, ImMap<String, T> params, final CompileSource compile) {
        ImMap<String, String> exprSource = params.mapValues(new GetValue<String, T>() {
            @Override
            public String getMapValue(T expr) {
                return expr.getSource(compile);
            }
        });

        Matcher m = paramsPattern.matcher(formula);
        StringBuffer result = new StringBuffer("(");
        while (m.find()) {
            String param = m.group();
            m.appendReplacement(result, Matcher.quoteReplacement(exprSource.get(param)));
        }
        m.appendTail(result);
        result.append(")");

        return result.toString();
    }

    public String getSource(CompileSource compile) {
        return castType.getCast(expr.getSource(compile), compile.syntax, false);
     }

    public Type getType(KeyType keyType) {
        return castType;
    }

    public Stat getTypeStat(KeyStat keyStat) {
        return getStaticClass().getTypeStat();
    }

    protected BaseExpr translate(MapTranslate translator) {
        return new CastExpr(expr.translateOuter(translator), castType);
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return create(expr.translateQuery(translator), castType);
    }

    public ConcreteClass getStaticClass() {
        return (DataClass) castType;
    }

    public boolean twins(TwinImmutableObject o) {
        return expr.equals(((CastExpr)o).expr) && castType.equals(((CastExpr)o).castType);
    }

    protected int hash(HashContext hashContext) {
        return 31 * expr.hashOuter(hashContext) + castType.hashCode();
    }

    protected boolean isComplex() {
        return true;
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return FormulaExpr.getStatValue(this, keyStat);
    }
    public InnerBaseJoin<?> getBaseJoin() {
        return new FormulaJoin<Integer>(MapFact.singleton(0, expr));
    }
}

