package platform.server.data.query.exprs;

import net.jcip.annotations.Immutable;
import platform.server.caches.Lazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.TwinLazy;
import platform.server.data.Table;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.SystemClass;
import platform.server.data.query.*;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Collections;

public class IsClassExpr extends StaticClassExpr {

    public VariableClassExpr expr;
    BaseClass baseClass;

    public IsClassExpr(VariableClassExpr iExpr, BaseClass iBaseClass) {
        expr = iExpr;
        
        baseClass = iBaseClass;
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
    public SourceExpr translateQuery(QueryTranslator translator) {
        return expr.translateQuery(translator).getClassExpr(baseClass); 
    }
    @ParamLazy
    public StaticClassExpr translateDirect(KeyTranslator translator) {
        return new IsClassExpr(expr.translateDirect(translator),baseClass);
    }

    public Where calculateWhere() {
        return expr.getWhere();
    }

    public int hashContext(HashContext hashContext) {
        return expr.hashContext(hashContext)+1;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return "class("+expr.getSource(compile)+")";

        return getJoinExpr().getSource(compile);
    }

    public void fillContext(Context context) {
        expr.fillContext(context);
    }

    public boolean twins(AbstractSourceJoin obj) {
        return expr.equals(((IsClassExpr)obj).expr) && baseClass.equals(((IsClassExpr)obj).baseClass);
    }
}
