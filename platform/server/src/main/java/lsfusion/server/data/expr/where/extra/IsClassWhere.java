package lsfusion.server.data.expr.where.extra;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.ParamLazy;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.ObjectValueClassSet;
import lsfusion.server.classes.ValueClassSet;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.ExprStatJoin;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

public class IsClassWhere extends DataWhere {

    private final SingleClassExpr expr;
    private final ValueClassSet classes;

    // для того чтобы один и тот же JoinSelect все использовали
    private final IsClassExpr classExpr;

    public IsClassWhere(SingleClassExpr expr, ValueClassSet classes) {
        this.expr = expr;
        this.classes = classes;

        assert !classes.isEmpty();
        if(classes instanceof ObjectValueClassSet)
            classExpr = new IsClassExpr(expr, (((ObjectValueClassSet) classes).getTables().keys())); // не через classExpr чтобы getWhere
        else
            classExpr = null;
    }

    public static Where create(SingleClassExpr expr, ValueClassSet classes) {
        if(classes instanceof ObjectValueClassSet) {
            ObjectValueClassSet objectClasses = (ObjectValueClassSet) packClassSet(Where.TRUE, expr, classes);
            if(objectClasses.getTables().size()> IsClassExpr.inlineThreshold)
                return new IsClassWhere(expr, classes);
            else
                return getTableWhere(expr, objectClasses);
        }
        return new IsClassWhere(expr, classes);
    }

    protected static Where getTableWhere(SingleClassExpr expr,ObjectValueClassSet classes) {
        Where result = Where.FALSE;
        ImSet<ObjectValueClassSet> tableChilds = classes.getTables().valuesSet();
        for(ObjectValueClassSet tableChild : tableChilds)
            result = result.exclOr(new IsClassWhere(expr, tableChild));
        return result;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return expr.getSource(compile) + " isClass(" + classes + ")";

        return ((ObjectValueClassSet)classes).getWhereString(classExpr.getSource(compile));
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    @Override
    protected String getNotSource(CompileSource compile) {
        return ((ObjectValueClassSet)classes).getNotWhereString(classExpr.getSource(compile));
    }

    protected Where translate(MapTranslate translator) {
        return new IsClassWhere(expr.translateOuter(translator),classes);
    }
    @ParamLazy
    public Where translateQuery(QueryTranslator translator) {
        return expr.translateQuery(translator).isClass(classes);
    }

    public static ValueClassSet getPackSet(Where outWhere, SingleClassExpr expr) {
        AndClassSet exprClasses = outWhere.and(expr.getWhere()).getClassWhere().getAndClassSet(expr);
        if(exprClasses==null)
            return null;
        return exprClasses.getValueClassSet();
    }

    private static ValueClassSet packClassSet(Where trueWhere, SingleClassExpr expr, ValueClassSet classes) {
        ValueClassSet packClasses;
        if(classes instanceof ObjectValueClassSet && ((ObjectValueClassSet) classes).getTables().size()>1 && ((packClasses = getPackSet(trueWhere, expr)) != null)) // проверка на ObjectValueClassSet - оптимизация
            classes = (ValueClassSet) packClasses.and(classes);
        return classes;
    }
    @Override
    public Where packFollowFalse(Where falseWhere) {
        return expr.packFollowFalse(falseWhere).isClass(packClassSet(falseWhere.not(), expr, classes)); // два раза будет паковаться, но может и правильно потому как expr меняется
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>singleton(expr);
    }

    protected void fillDataJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        if(classes instanceof ObjectValueClassSet)
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

    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, boolean noWhere) {
        if(classes instanceof ObjectValueClassSet) { // "модифицируем" статистику classExpr'а чтобы "уточнить статистику" по объектам
            // тут правда есть нюанс, что статистика по классам считается одним механизмом, а по join'ами другим
            Stat stat = classExpr.getStatValue().mult(new Stat(((ObjectValueClassSet) classes).getClassCount())).div(classExpr.getInnerJoin().getStatKeys(keyStat).rows);
            return new GroupJoinsWheres(new ExprStatJoin(classExpr, stat), this, noWhere);
        }
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
