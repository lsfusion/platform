package platform.server.data.query.wheres;

import platform.server.data.classes.DataClass;
import platform.server.data.classes.UnknownClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.classes.where.ObjectClassSet;
import platform.server.data.query.*;
import platform.server.data.query.exprs.IsClassExpr;
import platform.server.data.query.exprs.VariableClassExpr;
import platform.server.data.query.translators.Translator;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;
import platform.server.caches.ParamLazy;


public class IsClassWhere extends DataWhere {

    private final VariableClassExpr expr;
    private final AndClassSet classes;

    // для того чтобы один и тот же JoinSelect все использовали
    private final IsClassExpr classExpr;

    public IsClassWhere(VariableClassExpr iExpr, AndClassSet iClasses) {
        expr = iExpr;
        classes = iClasses;

        assert !classes.isEmpty();
        if(classes instanceof ObjectClassSet)
            classExpr = (IsClassExpr) iExpr.getClassExpr(((ObjectClassSet)classes).getBaseClass());
        else
            classExpr = null;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return expr.getSource(compile) + " isClass(" + classes + ")";

        return ((ObjectClassSet)classes).getWhereString(classExpr.getSource(compile));
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    @Override
    protected String getNotSource(CompileSource compile) {
        return ((ObjectClassSet)classes).getNotWhereString(classExpr.getSource(compile));
    }

    @ParamLazy
    public Where translate(Translator translator) {
        return expr.translate(translator).getIsClassWhere(classes);
    }

    public ClassExprWhere calculateClassWhere() {
        return new ClassExprWhere(expr,classes).and(expr.getWhere().getClassWhere());
    }

    public void fillContext(Context context) {
        expr.fillContext(context);
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
        if(!(classes instanceof UnknownClass || classes instanceof DataClass))
            return new InnerJoins(classExpr.joinExpr.getJoin(),this);
        return new InnerJoins(this);
    }

    public int hashContext(HashContext hashContext) {
        return expr.hashContext(hashContext) + classes.hashCode()*31;
    }

    public boolean equals(Object obj) {
        return this==obj || (obj instanceof IsClassWhere && expr.equals(((IsClassWhere)obj).expr) && classes.equals(((IsClassWhere)obj).classes));
    }
}
