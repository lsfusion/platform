package platform.server.data.expr;

import platform.server.caches.ParamLazy;
import platform.server.caches.TwinLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.Table;
import platform.server.classes.BaseClass;
import platform.server.classes.SystemClass;
import platform.server.data.query.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.Collections;

import net.jcip.annotations.Immutable;

@Immutable
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

    public DataWhereSet getFollows() {
        return expr.getFollows();
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(getJoinExpr(),andWhere);
        expr.fillJoinWheres(joins,andWhere);
    }

    public Type getType(Where where) {
        return SystemClass.instance;
    }

    public SystemClass getStaticClass() {
        return SystemClass.instance;
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return expr.translateQuery(translator).classExpr(baseClass);
    }
    @ParamLazy
    public StaticClassExpr translateDirect(KeyTranslator translator) {
        return new IsClassExpr(expr.translateDirect(translator),baseClass);
    }

    public Where calculateWhere() {
        return expr.isClass(baseClass.getUpSet());
    }

    public int hashContext(HashContext hashContext) {
        return expr.hashContext(hashContext)+1;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return "class("+expr.getSource(compile)+")";

        return getJoinExpr().getSource(compile);
    }

    public void enumerate(SourceEnumerator enumerator) {
        expr.enumerate(enumerator);
    }

    public boolean twins(AbstractSourceJoin obj) {
        return expr.equals(((IsClassExpr)obj).expr) && baseClass.equals(((IsClassExpr)obj).baseClass);
    }
}
