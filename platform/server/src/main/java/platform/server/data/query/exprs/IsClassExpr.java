package platform.server.data.query.exprs;

import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.SystemClass;
import platform.server.data.query.*;
import platform.server.data.query.translators.DirectTranslator;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.wheres.IsClassWhere;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Collections;
import java.util.Map;

public class IsClassExpr extends StaticClassExpr {

    public VariableClassExpr expr;
    BaseClass baseClass;

    public final JoinExpr joinExpr;

    IsClassExpr(VariableClassExpr iExpr, BaseClass iBaseClass) {
        expr = iExpr;
        
        baseClass = iBaseClass;

        // assertion что есть
        joinExpr = (JoinExpr) new DataJoin<KeyField, PropertyField>(baseClass.table,
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

    public SourceExpr translate(Translator translator) {
        return expr.translate(translator).getClassExpr(baseClass); 
    }

    public StaticClassExpr translateAnd(DirectTranslator translator) {
        return (IsClassExpr) expr.translateAnd(translator).getClassExpr(baseClass);
    }

    protected Where calculateWhere() {
        return new IsClassWhere(expr, baseClass.getUpSet());
    }

    public boolean equals(SourceExpr equalExpr, MapContext mapContext) {
        return equalExpr instanceof IsClassExpr && expr.equals(((IsClassExpr)equalExpr).expr, mapContext);
    }

    protected int getHash() {
        return expr.hash()+1;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return queryData.get(joinExpr);
    }

    public int fillContext(Context context, boolean compile) {
        if(compile)
            return context.add(joinExpr.from, compile);
        else
            return expr.fillContext(context, compile);
    }

    @Override
    public String toString() {
        return "class("+expr+")";
    }
}
