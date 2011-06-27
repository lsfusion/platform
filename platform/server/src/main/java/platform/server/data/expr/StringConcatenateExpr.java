package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.InsensitiveStringClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
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
        return getType(getWhere());
    }

    protected VariableExprSet calculateExprFollows() {
        return new VariableExprSet(exprs);
    }

    public BaseExpr translateOuter(MapTranslate translator) {
        return new StringConcatenateExpr(translator.translateDirect(exprs), separator, caseSensitive);
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

    public Where calculateWhere() {
        return getWhere(exprs);
    }

    public Expr translateQuery(QueryTranslator translator) {
        return create(translator.translate(exprs), separator, caseSensitive);
    }

    public boolean twins(TwinImmutableInterface obj) {
        return exprs.equals(((StringConcatenateExpr)obj).exprs);
    }

    public int hashOuter(HashContext hashContext) {
        int hash = 5;
        for(BaseExpr expr : exprs)
            hash = hash * 31 + expr.hashOuter(hashContext);
        return hash;
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

    public void enumDepends(ExprEnumerator enumerator) {
        enumerator.fill(exprs);
    }

    public long calculateComplexity() {
        return getComplexity(exprs);
    }
}
