package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.List;

// объединяет
public class StringConcatenateExpr extends StaticClassExpr {

    private List<BaseExpr> exprs;

    public StringConcatenateExpr(List<BaseExpr> exprs) {
        this.exprs = exprs;
    }

    public static Expr create(List<? extends Expr> exprs) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<Integer> mapCase : CaseExpr.pullCases(BaseUtils.toMap(exprs)))
            result.add(mapCase.where, BaseExpr.create(new StringConcatenateExpr(BaseUtils.toList(mapCase.data))));
        return result.getExpr();
    }

    @IdentityLazy
    public ConcreteClass getStaticClass() {
        return getType(getWhere());
    }

    protected VariableExprSet calculateExprFollows() {
        return new VariableExprSet(exprs);
    }

    public BaseExpr translateOuter(MapTranslate translator) {
        return new StringConcatenateExpr(translator.translateDirect(exprs));
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public StringClass getType(KeyType keyType) {
        int length = 0;
        for(BaseExpr expr : exprs)
            length += expr.getType(keyType).getBinaryLength(true);
        return StringClass.get(length);
    }

    public Where calculateWhere() {
        return getWhere(exprs);
    }

    public Expr translateQuery(QueryTranslator translator) {
        return create(translator.translate(exprs));
    }

    public boolean twins(AbstractSourceJoin obj) {
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
            if(exprType instanceof StringClass)
                exprSource = "rtrim(" + exprSource + ")";
            source = source + (source.length()==0?"":" || ' ' || ") + exprSource;
        }
        return "(" + source + ")";
    }

    public void enumerate(ContextEnumerator enumerator) {
        enumerator.fill(exprs);
    }
}
