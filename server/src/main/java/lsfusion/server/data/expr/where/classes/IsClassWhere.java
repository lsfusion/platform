package lsfusion.server.data.expr.where.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.ParamLazy;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NullableExpr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.classes.IsClassExpr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.classes.SingleClassExpr;
import lsfusion.server.data.expr.join.classes.IsClassField;
import lsfusion.server.data.expr.join.select.ExprStatJoin;
import lsfusion.server.data.expr.join.where.GroupJoinsWheres;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.JoinExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.action.session.classes.change.ClassChanges;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.physics.admin.Settings;

public class IsClassWhere extends DataWhere {

    private final SingleClassExpr expr;
    private final ValueClassSet classes;

    // для того чтобы один и тот же JoinSelect все использовали
    private final IsClassExpr classExpr;

    public final IsClassType type;
    
    public IsClassWhere(SingleClassExpr expr, ValueClassSet classes, IsClassType type) {
        this.expr = expr;
        this.classes = classes;
        this.type = type;

        assert !classes.isEmpty();
        if(isNotVirtualObject(classes, type))
            classExpr = new IsClassExpr(expr, (((ObjectValueClassSet) classes).getObjectClassFields().keys()), type); // не через classExpr чтобы getWhere
        else
            classExpr = null;
    }

    public static boolean isNotVirtualObject(ValueClassSet classes, IsClassType type) {
        return type != IsClassType.VIRTUAL && classes instanceof ObjectValueClassSet;
    }

    public static Where create(SingleClassExpr expr, ValueClassSet classes, IsClassType type) {
        if(isNotVirtualObject(classes, type)) {
            boolean inconsistent = type.isInconsistent();
            ObjectValueClassSet objectClasses = (ObjectValueClassSet) packClassSet(Where.TRUE(), expr, (ObjectValueClassSet)classes, inconsistent);
            ImRevMap<IsClassField, ObjectValueClassSet> tables;
            if(inconsistent)
                tables = BaseUtils.immutableCast(objectClasses.getObjectClassFields());
            else
                tables = objectClasses.getIsClassFields();
            if(tables.size() > Settings.get().getInlineClassThreshold())
                return new IsClassWhere(expr, classes, type);
            else
                return getTableWhere(expr, tables, type);
        }
        return new IsClassWhere(expr, classes, type);
    }

    private static Where getTableWhere(SingleClassExpr expr, ImMap<IsClassField, ObjectValueClassSet> tables, IsClassType type) {
        Where result = Where.FALSE();
        for(int i=0,size=tables.size();i<size;i++)
            result = result.or(tables.getKey(i).getIsClassWhere(expr, tables.getValue(i), type));
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
        if(classes instanceof StringClass) {
            String compareString;
            if(((StringClass) classes).length.isUnlimited())
                compareString = ">0";
            else
                compareString = "<= " + ((StringClass) classes).length;
            return "char_length(" + exprSource + ") " + compareString;
        }
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
        this.type = isClass.type;
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
        // getAdjustJoin в groupJoinsWheres нарушает инвариант создавая WhereJoin у которого появляется новый Expr (IsClassExpr) не в "пути" этого IsClassWhere и при проталкивании получаются проблемы
        if(classExpr != null && translator instanceof JoinExprTranslator) {
            Expr translatedClassExpr = classExpr.translateExpr(translator);
            if(translatedClassExpr instanceof KeyExpr) // если подставился ключ при проталкивании
                return ClassChanges.isStaticValueClass(translatedClassExpr, (ObjectValueClassSet)classes); // подставляем compare на конкретные классы N (IN ARRAY ???)
        }
        
        return expr.translateExpr(translator).isClass(classes, type);
    }

    public static ValueClassSet getPackSet(Where outWhere, SingleClassExpr expr) {
        AndClassSet exprClasses = outWhere.and(expr.getWhere()).getClassWhere().getAndClassSet(expr);
        if(exprClasses==null)
            return null;
        return exprClasses.getValueClassSet();
    }

    private static ValueClassSet packClassSet(Where trueWhere, SingleClassExpr expr, ObjectValueClassSet classes, boolean inconsistent) {
        ValueClassSet resultClasses = classes;
        ValueClassSet packClasses;
        if(!inconsistent && classes.getIsClassFields().size()>1 && ((packClasses = getPackSet(trueWhere, expr)) != null)) // проверка на ObjectValueClassSet - оптимизация
            resultClasses = (ValueClassSet) packClasses.and(resultClasses);
        return resultClasses;
    }
    @Override
    public Where packFollowFalse(Where falseWhere) {
        ValueClassSet packedClasses = classes;
        if(isNotVirtualObject(packedClasses, type))
            packedClasses = packClassSet(falseWhere.not(), expr, (ObjectValueClassSet)packedClasses, type.isInconsistent());
        return expr.packFollowFalse(falseWhere).isClass(packedClasses, type); // will be packed twice, but it's probably ok, since expr can be changed
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>singleton(expr);
    }

    protected void fillDataJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
        if(classExpr != null)
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
        if(classExpr != null) { // "модифицируем" статистику classExpr'а чтобы "уточнить статистику" по объектам
            // тут правда есть нюанс, что статистика по классам считается одним механизмом, а по join'ами другим
            ExprStatJoin adjustJoin = classExpr.getAdjustStatJoin(new Stat(((ObjectValueClassSet) classes).getCount()), keyStat, statType, false);
            return groupDataJoinsWheres(adjustJoin, type);
        }
        return expr.getNotNullWhere().groupJoinsWheres(keepStat, statType, keyStat, orderTop, type).and(super.groupJoinsWheres(keepStat, statType, keyStat, orderTop, type));
    }
    public ClassExprWhere calculateClassWhere() {
        return expr.getClassWhere(type.isInconsistent() ? getBaseClass().getUpSet() : classes).and(expr.getNotNullClassWhere());
    }

    public int hash(HashContext hashContext) {
        return (expr.hashOuter(hashContext) ^ classes.hashCode()*31) + type.hashCode();
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return expr.equals(((IsClassWhere)obj).expr) && classes.equals(((IsClassWhere)obj).classes) && type.equals(((IsClassWhere)obj).type);
    }

    @Override
    public boolean isClassWhere() {
        return !type.isInconsistent();
    }
}
