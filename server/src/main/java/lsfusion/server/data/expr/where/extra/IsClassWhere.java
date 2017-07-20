package lsfusion.server.data.expr.where.extra;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.ParamLazy;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.ExprStatJoin;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.ExprTranslator;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.property.IsClassField;

public class IsClassWhere extends DataWhere {

    private final SingleClassExpr expr;
    private final ValueClassSet classes;

    // для того чтобы один и тот же JoinSelect все использовали
    private final IsClassExpr classExpr;

    public final boolean inconsistent; // для проверки / перерасчета классов и определения DataClass для неизвестного объекта

    public IsClassWhere(SingleClassExpr expr, ValueClassSet classes, boolean inconsistent) {
        this.expr = expr;
        this.classes = classes;
        this.inconsistent = inconsistent;

        assert !classes.isEmpty();
        if(classes instanceof ObjectValueClassSet)
            classExpr = new IsClassExpr(expr, (((ObjectValueClassSet) classes).getObjectClassFields().keys()), inconsistent ? IsClassType.INCONSISTENT : IsClassType.CONSISTENT); // не через classExpr чтобы getWhere
        else
            classExpr = null;
    }

    public static Where create(SingleClassExpr expr, ValueClassSet classes, boolean inconsistent) {
        if(classes instanceof ObjectValueClassSet) {
            ObjectValueClassSet objectClasses = (ObjectValueClassSet) packClassSet(Where.TRUE, expr, classes, inconsistent);
            ImRevMap<IsClassField, ObjectValueClassSet> tables;
            if(inconsistent)
                tables = BaseUtils.immutableCast(objectClasses.getObjectClassFields());
            else
                tables = objectClasses.getIsClassFields();
            if(tables.size()> IsClassExpr.inlineThreshold)
                return new IsClassWhere(expr, classes, inconsistent);
            else
                return getTableWhere(expr, tables, inconsistent);
        }
        return new IsClassWhere(expr, classes, inconsistent);
    }

    private static Where getTableWhere(SingleClassExpr expr, ImMap<IsClassField, ObjectValueClassSet> tables, boolean notConsistent) {
        Where result = Where.FALSE;
        for(int i=0,size=tables.size();i<size;i++)
            result = result.or(tables.getKey(i).getIsClassWhere(expr, tables.getValue(i), notConsistent));
        return result;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return expr.getSource(compile) + " isClass(" + classes + ")";

        if(classes instanceof ObjectValueClassSet) // основной case
            return ((ObjectValueClassSet)classes).getWhereString(classExpr.getSource(compile));
        // редкая ситуация, связанная с приведением типов в меньшую сторону
        assert classExpr == null;
        String exprSource = expr.getSource(compile);
        if(classes instanceof StringClass)  
            return "char_length(" + exprSource + ") <= " + ((StringClass)classes).length;
        if(classes instanceof IntegralClass) {
            IntegralClass integralClass = (IntegralClass) classes;
            return exprSource + " BETWEEN " + integralClass.getString(integralClass.getInfiniteValue(true), compile.syntax) + " AND " + integralClass.getString(integralClass.getInfiniteValue(false), compile.syntax);
        } 
        throw new UnsupportedOperationException();
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    @Override
    protected String getNotSource(CompileSource compile) {
        return ((ObjectValueClassSet)classes).getNotWhereString(classExpr.getSource(compile));
    }

    private IsClassWhere(IsClassWhere isClass, MapTranslate translate) {
        this.expr = isClass.expr.translateOuter(translate);
        this.classes = isClass.classes;
        this.inconsistent = isClass.inconsistent;
        if(isClass.classExpr!=null)
            classExpr = (IsClassExpr) isClass.classExpr.translateOuter(translate);
        else
            classExpr = null;
    }

    protected Where translate(MapTranslate translator) {
        return new IsClassWhere(this, translator);
    }
    @ParamLazy
    public Where translate(ExprTranslator translator) {
        return expr.translateExpr(translator).isClass(classes, inconsistent);
    }

    public static ValueClassSet getPackSet(Where outWhere, SingleClassExpr expr) {
        AndClassSet exprClasses = outWhere.and(expr.getWhere()).getClassWhere().getAndClassSet(expr);
        if(exprClasses==null)
            return null;
        return exprClasses.getValueClassSet();
    }

    private static ValueClassSet packClassSet(Where trueWhere, SingleClassExpr expr, ValueClassSet classes, boolean inconsistent) {
        ValueClassSet packClasses;
        if(!inconsistent && classes instanceof ObjectValueClassSet && ((ObjectValueClassSet) classes).getIsClassFields().size()>1 && ((packClasses = getPackSet(trueWhere, expr)) != null)) // проверка на ObjectValueClassSet - оптимизация
            classes = (ValueClassSet) packClasses.and(classes);
        return classes;
    }
    @Override
    public Where packFollowFalse(Where falseWhere) {
        return expr.packFollowFalse(falseWhere).isClass(packClassSet(falseWhere.not(), expr, classes, inconsistent), inconsistent); // два раза будет паковаться, но может и правильно потому как expr меняется
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

    protected ImSet<NullableExprInterface> getExprFollows() {
        return expr.getExprFollows(true, NullableExpr.FOLLOW, true);
    }

    //    public KeyEquals calculateKeyEquals() {
//        return expr.getWhere().getKeyEquals().and(new KeyEquals(this));
//    }

    private BaseClass getBaseClass() {
        return ((ObjectValueClassSet)classes).getBaseClass();
    }

    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, StatType statType, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        if(classes instanceof ObjectValueClassSet) { // "модифицируем" статистику classExpr'а чтобы "уточнить статистику" по объектам
            // тут правда есть нюанс, что статистика по классам считается одним механизмом, а по join'ами другим
            ExprStatJoin adjustJoin = classExpr.getAdjustStatJoin(new Stat(((ObjectValueClassSet) classes).getCount()), keyStat, statType, false);
            return groupDataJoinsWheres(adjustJoin, type);
        }
        return expr.getNotNullWhere().groupJoinsWheres(keepStat, statType, keyStat, orderTop, type).and(super.groupJoinsWheres(keepStat, statType, keyStat, orderTop, type));
    }
    public ClassExprWhere calculateClassWhere() {
        return expr.getClassWhere(inconsistent ? getBaseClass().getUpSet() : classes).and(expr.getWhere().getClassWhere());
    }

    public int hash(HashContext hashContext) {
        return (expr.hashOuter(hashContext) ^ classes.hashCode()*31) + (inconsistent ? 1 : 0);
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return expr.equals(((IsClassWhere)obj).expr) && classes.equals(((IsClassWhere)obj).classes) && inconsistent == (((IsClassWhere)obj).inconsistent);
    }

    @Override
    public boolean isClassWhere() {
        return !inconsistent;
    }
}
