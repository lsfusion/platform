package platform.server.data.expr;

import platform.server.caches.ParamLazy;
import platform.server.caches.TwinLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.classes.CustomObjectClass;
import platform.server.data.Table;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.Collections;

public class IsClassExpr extends StaticClassExpr {

    public final SingleClassExpr expr;
    final BaseClass baseClass;

    public IsClassExpr(SingleClassExpr expr, BaseClass baseClass) {
        this.expr = expr;
        
        this.baseClass = baseClass;
    }

    @TwinLazy
    public Table.Join.Expr getJoinExpr() {
        return (Table.Join.Expr) baseClass.table.joinAnd(
                Collections.singletonMap(baseClass.table.key, expr)).getExpr(baseClass.table.objectClass);
    }

    public VariableExprSet calculateExprFollows() {
        VariableExprSet result = new VariableExprSet(expr.getExprFollows());
        result.add(getJoinExpr());
        return result;
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(getJoinExpr(),andWhere);
        expr.fillJoinWheres(joins,andWhere);
    }

    public Type getType(KeyType keyType) {
        return getStaticClass().getType();
    }

    public CustomObjectClass getStaticClass() {
        return baseClass.objectClass;
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return expr.translateQuery(translator).classExpr(baseClass);
    }
    @Override
    public Expr packFollowFalse(Where where) {
        return expr.packFollowFalse(where).classExpr(baseClass);
    }
    @ParamLazy
    public StaticClassExpr translateOuter(MapTranslate translator) {
        return new IsClassExpr(expr.translateOuter(translator),baseClass);
    }

    public Where calculateWhere() {
        return expr.isClass(baseClass.getUpSet());
    }

    public int hashOuter(HashContext hashContext) {
        return expr.hashOuter(hashContext)+1;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return "class("+expr.getSource(compile)+")";

        return getJoinExpr().getSource(compile);
    }

    public void enumerate(ContextEnumerator enumerator) {
        expr.enumerate(enumerator);
    }

    public boolean twins(AbstractSourceJoin obj) {
        return expr.equals(((IsClassExpr)obj).expr) && baseClass.equals(((IsClassExpr)obj).baseClass);
    }

    public long calculateComplexity() {
        return expr.getComplexity() + 1;
    }
}
