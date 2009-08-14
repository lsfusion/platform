package platform.server.data.query.exprs;

import platform.server.data.Table;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.SystemClass;
import platform.server.data.query.*;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;
import platform.server.caches.ParamLazy;

import java.util.Collections;

public class IsClassExpr extends StaticClassExpr {

    public VariableClassExpr expr;
    BaseClass baseClass;

    public final Table.Join.Expr joinExpr;

    public IsClassExpr(VariableClassExpr iExpr, BaseClass iBaseClass) {
        expr = iExpr;
        
        baseClass = iBaseClass;

        // assertion что есть
        joinExpr = (Table.Join.Expr) baseClass.table.joinAnd(
                Collections.singletonMap(baseClass.table.key, expr)).getExpr(baseClass.table.objectClass);
    }

    public DataWhereSet getFollows() {
        return expr.getFollows();
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(joinExpr,andWhere);
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
        return expr.translate(translator).getClassExpr(baseClass); 
    }

    @ParamLazy
    public StaticClassExpr translateDirect(KeyTranslator translator) {
        return (IsClassExpr) expr.translateDirect(translator).getClassExpr(baseClass);
    }

    protected Where calculateWhere() {
        return expr.getWhere();
    }

    public int hashContext(HashContext hashContext) {
        return expr.hashContext(hashContext)+1;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return "class("+expr.getSource(compile)+")";

        return joinExpr.getSource(compile);
    }

    public void fillContext(Context context) {
        expr.fillContext(context);
    }
}
