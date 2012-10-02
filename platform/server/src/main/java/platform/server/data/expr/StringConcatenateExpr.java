package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.InsensitiveStringClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.query.stat.CalculateJoin;
import platform.server.data.query.stat.FormulaJoin;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.List;
import java.util.Map;

// объединяет
public class StringConcatenateExpr extends StaticClassExpr {

    private List<BaseExpr> exprs;
    private final String separator;
    private final boolean caseSensitive;

    public StringConcatenateExpr(List<BaseExpr> exprs, String separator, boolean caseSensitive) {
        this.exprs = exprs;
        this.separator = separator;
        this.caseSensitive = caseSensitive;
    }

    public static Expr create(List<? extends Expr> exprs, final String separator, final boolean caseSensitive) {
        return new ExprPullWheres<Integer>() {
            protected Expr proceedBase(Map<Integer, BaseExpr> map) {
                return BaseExpr.create(new StringConcatenateExpr(BaseUtils.toList(map), separator, caseSensitive));
            }
        }.proceed(BaseUtils.toMap(exprs));
    }

    @IdentityLazy
    public ConcreteClass getStaticClass() {
        return (ConcreteClass) FormulaExpr.getCompatibleType(exprs); // есть еще вариант как в Union'е сделать с явным классом
//        return getType(getWhere());
    }

    protected StringConcatenateExpr translate(MapTranslate translator) {
        return new StringConcatenateExpr(translator.translateDirect(exprs), separator, caseSensitive);
    }

    protected boolean isComplex() {
        return true;
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(BaseExpr param : exprs)
            param.fillJoinWheres(joins, andWhere);
    }

    public StringClass getType(KeyType keyType) {
        int length = 0;
        for(BaseExpr expr : exprs)
            length += expr.getType(keyType).getBinaryLength(true);
        return caseSensitive ? StringClass.get(length) : InsensitiveStringClass.get(length);
    }
    public Stat getTypeStat(KeyStat keyStat) {
        Stat result = Stat.ONE;
        for(BaseExpr expr : exprs)
            result = result.mult(expr.getTypeStat(keyStat));
        return result;
    }

    @Override
    public Expr translateQuery(QueryTranslator translator) {
        return create(translator.translate(exprs), separator, caseSensitive);
    }

    public boolean twins(TwinImmutableInterface obj) {
        return exprs.equals(((StringConcatenateExpr) obj).exprs) && separator.equals(((StringConcatenateExpr)obj).separator) && caseSensitive == ((StringConcatenateExpr)obj).caseSensitive;
    }

    public int hash(HashContext hashContext) {
        return 31 * (31 * hashOuter(exprs, hashContext) + separator.hashCode()) + (caseSensitive ? 1 : 0);
    }

    public String getSource(CompileSource compile) {
        String source = "";
        for(BaseExpr expr : exprs) {
            Type exprType = expr.getType(compile.keyType);
            String exprSource = expr.getSource(compile);
            if(exprType instanceof StringClass) {
                exprSource = "rtrim(" + exprSource + ")";
            }
            source = source + (source.length()==0?"":" || '" + separator + "' || ") + exprSource;
        }
        return "(" + source + ")";
    }

    public Stat getStatValue(KeyStat keyStat) {
        return FormulaExpr.getStatValue(this, keyStat);
    }
    public InnerBaseJoin<?> getBaseJoin() {
        return new FormulaJoin<Integer>(BaseUtils.toMap(exprs));
    }
}
