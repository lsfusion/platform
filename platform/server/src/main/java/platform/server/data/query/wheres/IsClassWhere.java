package platform.server.data.query.wheres;

import platform.server.data.classes.UnknownClass;
import platform.server.data.classes.DataClass;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.classes.where.ObjectClassSet;
import platform.server.data.classes.where.ClassSet;
import platform.server.data.query.*;
import platform.server.data.query.exprs.IsClassExpr;
import platform.server.data.query.exprs.VariableClassExpr;
import platform.server.data.query.translators.Translator;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Map;


public class IsClassWhere extends DataWhere {

    private final VariableClassExpr expr;
    private final ClassSet classes;

    // для того чтобы один и тот же Join все использовали
    private final IsClassExpr classExpr;

    public IsClassWhere(VariableClassExpr iExpr, ClassSet iClasses) {
        expr = iExpr;
        classes = iClasses;

        assert !classes.isEmpty();
        if(classes instanceof ObjectClassSet)
            classExpr = (IsClassExpr) iExpr.getClassExpr(((ObjectClassSet)classes).getBaseClass());
        else
            classExpr = null;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return ((ObjectClassSet)classes).getWhereString(classExpr.getSource(queryData, syntax));
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    protected String getNotSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return ((ObjectClassSet)classes).getNotWhereString(classExpr.getSource(queryData, syntax));
    }

    public String toString() {
        return expr.toString() + " isClass(" + classes + ")";
    }

    public Where translate(Translator translator) {
        return expr.translate(translator).getIsClassWhere(classes);
    }

    public ClassExprWhere calculateClassWhere() {
        return new ClassExprWhere(expr,classes).and(expr.getWhere().getClassWhere());
    }

    public int fillContext(Context context, boolean compile) {
        if(classes instanceof ObjectClassSet)
            return classExpr.fillContext(context, compile);
        else
            return expr.fillContext(context, compile);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        if(classes instanceof ObjectClassSet)
            classExpr.fillJoinWheres(joins,andWhere);
        else
            expr.fillJoinWheres(joins,andWhere);        
    }

    protected DataWhereSet getExprFollows() {
        return expr.getFollows();
    }

    public InnerJoins getInnerJoins() {
        Where joinWhere = Where.TRUE;
        if(classes instanceof DataClass)
            joinWhere = this;
        else
        if(!(classes instanceof UnknownClass))
            joinWhere = classExpr.joinExpr.from.getWhere();
        return new InnerJoins(joinWhere,this);
    }

    // для кэша
    public boolean equals(Where where, MapContext mapContext) {
        return where instanceof IsClassWhere && classes.equals(((IsClassWhere)where).classes) &&
                expr.equals(((IsClassWhere)where).expr, mapContext);
    }

    protected int getHash() {
        return expr.hash() + classes.hashCode()*31;
    }

    protected int getHashCode() {
        return expr.hashCode() + classes.hashCode()*31;
    }

    public boolean equals(Object obj) {
        return this==obj || (obj instanceof IsClassWhere && expr.equals(((IsClassWhere)obj).expr) && classes.equals(((IsClassWhere)obj).classes));
    }
}
