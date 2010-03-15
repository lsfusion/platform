package platform.server.data.expr.where;

import platform.server.caches.ParamLazy;
import platform.server.caches.HashContext;
import platform.server.classes.DataClass;
import platform.server.classes.UnknownClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.ObjectClassSet;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.query.*;
import platform.server.data.expr.IsClassExpr;
import platform.server.data.expr.VariableClassExpr;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;


public class IsClassWhere extends DataWhere {

    private final VariableClassExpr expr;
    private final AndClassSet classes;

    // для того чтобы один и тот же JoinSelect все использовали
    private final IsClassExpr classExpr;

    public IsClassWhere(VariableClassExpr expr, AndClassSet classes) {
        this.expr = expr;
        this.classes = classes;

        assert !this.classes.isEmpty();
        if(this.classes instanceof ObjectClassSet)
            classExpr = (IsClassExpr) expr.classExpr(((ObjectClassSet) this.classes).getBaseClass());
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
    public Where translateDirect(KeyTranslator translator) {
        return new IsClassWhere(expr.translateDirect(translator),classes);
    }
    @ParamLazy
    public Where translateQuery(QueryTranslator translator) {
        return expr.translateQuery(translator).isClass(classes);
    }

    public void enumerate(SourceEnumerator enumerator) {
        expr.enumerate(enumerator);
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
            return new InnerJoins(classExpr.getJoinExpr().getJoin(),this);
        return new InnerJoins(this);
    }
    public ClassExprWhere calculateClassWhere() {
        return new ClassExprWhere(expr,classes).and(expr.getWhere().getClassWhere());
    }

    public int hashContext(HashContext hashContext) {
        return expr.hashContext(hashContext) + classes.hashCode()*31;
    }

    public boolean twins(AbstractSourceJoin obj) {
        return expr.equals(((IsClassWhere)obj).expr) && classes.equals(((IsClassWhere)obj).classes);
    }
}
