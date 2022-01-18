package lsfusion.server.data.expr.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.base.caches.ParamLazy;
import lsfusion.server.base.caches.TwinLazy;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.*;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.join.inner.InnerJoin;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.query.SubQueryExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.expr.where.classes.IsClassWhere;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;

public class IsClassExpr extends InnerExpr implements StaticClassExprInterface {

    public final SingleClassExpr expr;
    private final ImSet<ObjectClassField> classTables;

    private final IsClassType type; // для проверки / перерасчета классов и определения DataClass для неизвестного объекта

    public IsClassExpr(SingleClassExpr expr, ImSet<ObjectClassField> classTables, IsClassType type) {
        this.expr = expr;

        this.classTables = classTables;
        this.type = type;
    }

    public boolean isComplex() {
        return classTables.size() > 0;
    }

    public static final int subqueryThreshold = 6;
    public InnerExpr getInnerJoinExpr() {
        return (InnerExpr) getJoinExpr();
    }
    @TwinLazy
    public Expr getJoinExpr() {
        if(classTables.size()==1) {
            ObjectClassField classTable = classTables.single();
            if(type.isInconsistent())
                return classTable.getInconsistentExpr(expr);
            else
                return classTable.getStoredExpr(expr);
        } else {
            assert classTables.size() > inlineThreshold;
            // будем делить на inlineThreshold корзин
            Pair<KeyExpr,Expr> subQuery = getBaseClass().getSubQuery(classTables, type);
            return new SubQueryExpr(new SubQueryExpr.Query(subQuery.second, false, 0), MapFact.singleton(subQuery.first, expr));
        }
    }

    public static final int inlineThreshold = 4;
    public static Expr create(SingleClassExpr expr, ImSet<ObjectClassField> classes, IsClassType type) {
        classes = packTables(Where.TRUE(), expr, classes, type);

        if(classes.size()> inlineThreshold || classes.size()==1)
            return getExpr(type, BaseExpr.create(new IsClassExpr(expr, classes, type)));
        else
            return getTableExpr(expr, classes, inlineThreshold, type);
    }

    public static Expr getTableExpr(SingleClassExpr expr, ImSet<ObjectClassField> classTables, final int threshold, IsClassType type) {
        final ImOrderSet<ObjectClassField> orderClassTables = classTables.toOrderSet();
        CaseExprInterface mCases = null;
        MLinearOperandMap mLinear = null;
        MList<Expr> mAgg =  null;

        ImMap<Integer, ImSet<ObjectClassField>> group = classTables.group(new BaseUtils.Group<Integer, ObjectClassField>() {
            public Integer group(ObjectClassField key) {
                return orderClassTables.indexOf(key) % threshold;
            }});

        switch (type) {
            case SUMCONSISTENT:
                mLinear = new MLinearOperandMap();
                break;
            case AGGCONSISTENT:
                mAgg = ListFact.mList();
                break;
            default:
                mCases = Expr.newCases(true, group.size());
        }

//        IsClassWhere[] classWheres = new IsClassWhere[group.size()];
        for(int i=0,size=group.size();i<size;i++) {
            Expr classExpr = create(expr, group.get(i), type);

            if(classTables.size()==1) // оптимизация и разрыв рекурсии
                return classExpr;

            Where where = classExpr.getWhere();

            switch (type) {
                case SUMCONSISTENT:
                    mLinear.add(classExpr, 1);
                    break;
                case AGGCONSISTENT:
                    mAgg.add(classExpr);
                    break;
                default:
                    mCases.add(where, classExpr);
            }
//            classWheres[i] = where;
        }
        Expr result;

        switch (type) {
            case SUMCONSISTENT:
                result = mLinear.getExpr();
                break;
            case AGGCONSISTENT:
                result = FormulaUnionExpr.create(new ConcatenateFormulaImpl(","), mAgg.immutableList());
                break;
            default:
                result = mCases.getFinal();
        }

//        result.setWhere(AbstractWhere.toWhere(classWheres));

        return result;
    }

    private static Expr getExpr(IsClassType type, Expr classExpr) {
        if(type==IsClassType.AGGCONSISTENT)
            classExpr = FormulaExpr.create(new CastFormulaImpl(StringClass.getv(false, ExtInt.UNLIMITED)), ListFact.singleton(classExpr));

        if(type==IsClassType.SUMCONSISTENT)
            classExpr = ValueExpr.COUNT.and(classExpr.getWhere());
        return classExpr;
    }

    /*    public void fillAndJoinWheres(MapWhere<FJData> joins, Where andWhere) {
        joins.add(getJoinExpr(),andWhere);
        expr.fillJoinWheres(joins,andWhere);
    }*/

    public Type getType(KeyType keyType) {
        return getStaticClass(keyType).getType();
    }
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return getStaticClass().getTypeStat(forJoin);
    }

    public ConcreteClass getStaticClass() {
        switch(type) {
            case SUMCONSISTENT:
                return ValueExpr.COUNTCLASS;
            case AGGCONSISTENT:
                return StringClass.getv(false, ExtInt.UNLIMITED);
        }
        return getBaseClass().objectClass;
    }

    @Override
    public ConcreteClass getStaticClass(KeyType keyType) {
        return getStaticClass();
    }

    private BaseClass getBaseClass() {
        return classTables.get(0).getObjectSet().getBaseClass();
    }

    @ParamLazy
    public Expr translate(ExprTranslator translator) {
        return expr.translateExpr(translator).classExpr(classTables, type);
    }

    private static ImSet<ObjectClassField> packTables(Where trueWhere, SingleClassExpr expr, ImSet<ObjectClassField> tables, IsClassType type) {
        if(!type.isInconsistent() && tables.size() > 1) {
            final ValueClassSet exprClasses = IsClassWhere.getPackSet(trueWhere, expr);
            if(exprClasses != null)
                tables = tables.filterFn(element -> !element.getObjectSet().and(exprClasses).isEmpty());
        }
        return tables;
    }
    @Override
    public Expr packFollowFalse(Where where) {
        return expr.packFollowFalse(where).classExpr(packTables(where.not(), expr, classTables, type), type); // два раза будет паковаться, но может и правильно потому как expr меняется
    }
    protected IsClassExpr translate(MapTranslate translator) {
        return new IsClassExpr(expr.translateOuter(translator), classTables, type);
    }

    public static boolean inSet(ConcreteObjectClass staticClass, ImSet<ObjectClassField> classTables) {
        for(ObjectClassField classTable : classTables)
            if(staticClass.inSet(classTable.getObjectSet()))
                return true;
        return false;
    }

    private ObjectValueClassSet getObjectSet() {
        return getBaseClass().getSet(classTables);
    }

    public Where calculateNotNullWhere() {
        return expr.isClass(getObjectSet(), type);
    }

    public int hash(HashContext hashContext) {
        return expr.hashOuter(hashContext) + type.hashCode();
    }

    public String getSource(CompileSource compile, boolean needValue) {
        return compile.getSource(this, needValue);
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return expr.equals(((IsClassExpr)obj).expr) && classTables.equals(((IsClassExpr)obj).classTables) && type.equals(((IsClassExpr)obj).type);
    }

    public Stat getStatValue(StatType statType) {
        return getAdjustStatValue(statType, new Stat(getObjectSet().getClassCount()));
    }

    public PropStat getInnerStatValue(KeyStat keyStat, StatType type) {
        return new PropStat(getStatValue(type));
    }
    public InnerJoin<?, ?> getInnerJoin() {
        return getInnerJoinExpr().getInnerJoin();
    }
    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.singleton(getInnerJoinExpr());
    }

    // множественное наследование StaticClassExpr
    @Override
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return StaticClassExpr.getClassWhere(this, classes);
    }

    @Override
    public Expr classExpr(ImSet<ObjectClassField> classes, IsClassType type) {
        return StaticClassExpr.classExpr(this, classes, type);
    }

    @Override
    public Where isClass(ValueClassSet set, IsClassType type) {
        return StaticClassExpr.isClass(this, set, type);
    }

    @Override
    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        return StaticClassExpr.getAndClassSet(this, and);
    }

    @Override
    public boolean addAndClassSet(MMap<VariableSingleClassExpr, AndClassSet> and, AndClassSet add) {
        return StaticClassExpr.addAndClassSet(this, add);
    }

}
