package platform.server.data.expr.where.extra;

import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.OuterContext;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.classes.ObjectValueClassSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.ObjectClassSet;
import platform.server.data.expr.*;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprStatJoin;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

public class IsClassWhere extends DataWhere {

    private final SingleClassExpr expr;
    private final AndClassSet classes;

    // для того чтобы один и тот же JoinSelect все использовали
    private final IsClassExpr classExpr;

    public IsClassWhere(SingleClassExpr expr, AndClassSet classes) {
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

    protected Where translate(MapTranslate translator) {
        return new IsClassWhere(expr.translateOuter(translator),classes);
    }
    @ParamLazy
    public Where translateQuery(QueryTranslator translator) {
        return expr.translateQuery(translator).isClass(classes);
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>singleton(expr);
    }

    protected void fillDataJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        if(classes instanceof ObjectClassSet)
            classExpr.fillJoinWheres(joins,andWhere);
        else
            expr.fillJoinWheres(joins,andWhere);        
    }

    protected ImSet<DataWhere> calculateFollows() {
        return NotNullExpr.getFollows(expr.getExprFollows(true, true));
    }

    //    public KeyEquals calculateKeyEquals() {
//        return expr.getWhere().getKeyEquals().and(new KeyEquals(this));
//    }

    private static Stat getClassStat(ObjectValueClassSet classes) { // "модифицируем" статистику classExpr'а чтобы правильно расчитывала кол-во объектов
        BaseClass baseClass = classes.getBaseClass();
        return new Stat((double) (classes.getCount() * baseClass.objectClass.getCount()) / (double) baseClass.getCount());
    }
    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, boolean noWhere) {
        if(classes instanceof ObjectValueClassSet)
            return new GroupJoinsWheres(new ExprStatJoin(classExpr, getClassStat((ObjectValueClassSet)classes)), this, noWhere);
        return expr.getWhere().groupJoinsWheres(keepStat, keyStat, orderTop, noWhere).and(new GroupJoinsWheres(this, noWhere));
    }
    public ClassExprWhere calculateClassWhere() {
        return expr.getClassWhere(classes).and(expr.getWhere().getClassWhere());
    }

    public int hash(HashContext hashContext) {
        return expr.hashOuter(hashContext) ^ classes.hashCode()*31;
    }

    public boolean twins(TwinImmutableObject obj) {
        return expr.equals(((IsClassWhere)obj).expr) && classes.equals(((IsClassWhere)obj).classes);
    }

}
