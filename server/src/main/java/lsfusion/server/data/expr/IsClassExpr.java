package lsfusion.server.data.expr;

import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.ParamLazy;
import lsfusion.server.caches.TwinLazy;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.formula.CastFormulaImpl;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.formula.StringAggConcatenateFormulaImpl;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.SubQueryExpr;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.expr.where.extra.IsClassWhere;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.InnerJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.property.ClassField;

public class IsClassExpr extends InnerExpr implements StaticClassExprInterface {

    public final SingleClassExpr expr;
    private final ImSet<ClassField> classTables;

    private final IsClassType type; // для проверки / перерасчета классов и определения DataClass для неизвестного объекта

    public IsClassExpr(SingleClassExpr expr, ImSet<ClassField> classTables, IsClassType type) {
        this.expr = expr;

        this.classTables = classTables;
        this.type = type;
    }

    public static final int subqueryThreshold = 6;
    @TwinLazy
    public InnerExpr getJoinExpr() {
        if(classTables.size()==1) {
            ClassField classTable = classTables.single();
            if(type.isInconsistent())
                return classTable.getInconsistentExpr(expr);
            else
                return classTable.getStoredExpr(expr);
        } else {
            assert classTables.size() > inlineThreshold;
            // будем делить на inlineThreshold корзин
            Pair<KeyExpr,Expr> subQuery = getBaseClass().getSubQuery(classTables, type);
            return new SubQueryExpr(subQuery.second, MapFact.singleton(subQuery.first, (BaseExpr) expr));
        }
    }

    public static final int inlineThreshold = 4;
    public static Expr create(SingleClassExpr expr, ImSet<ClassField> classes, IsClassType type) {
        classes = packTables(Where.TRUE, expr, classes, type);

        if(classes.size()> inlineThreshold || classes.size()==1)
            return new IsClassExpr(expr, classes, type);
        else
            return getTableExpr(expr, classes, inlineThreshold, type);
    }

    public static Expr getTableExpr(SingleClassExpr expr, ImSet<ClassField> classTables, final int threshold, IsClassType type) {
        final ImOrderSet<ClassField> orderClassTables = classTables.toOrderSet();
        CaseExprInterface mCases = null;
        MLinearOperandMap mLinear = null;
        MList<Expr> mAgg =  null;

        switch (type) {
            case SUMCONSISTENT:
                mLinear = new MLinearOperandMap();
                break;
            case AGGCONSISTENT:
                mAgg = ListFact.mList();
                break;
            default:
                mCases = Expr.newCases(true);
        }

        ImMap<Integer, ImSet<ClassField>> group = classTables.group(new BaseUtils.Group<Integer, ClassField>() {
            public Integer group(ClassField key) {
                return orderClassTables.indexOf(key) % threshold;
            }});

//        IsClassWhere[] classWheres = new IsClassWhere[group.size()];
        for(int i=0,size=group.size();i<size;i++) {
            Expr classExpr = create(expr, group.get(i), type);
            if(type==IsClassType.AGGCONSISTENT)
                classExpr = FormulaExpr.create(new CastFormulaImpl(StringClass.getv(false, ExtInt.UNLIMITED)), ListFact.singleton(classExpr));

            Where where = classExpr.getWhere();

            if(type==IsClassType.SUMCONSISTENT)
                classExpr = ValueExpr.COUNT.and(where);  

            if(classTables.size()==1) // оптимизация и разрыв рекурсии
                return classExpr;

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
                result = FormulaUnionExpr.create(new StringAggConcatenateFormulaImpl(","), mAgg.immutableList());
                break;
            default:
                result = mCases.getFinal();
        }

//        result.setWhere(AbstractWhere.toWhere(classWheres));

        return result;
    }

    /*    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(getJoinExpr(),andWhere);
        expr.fillJoinWheres(joins,andWhere);
    }*/

    public Type getType(KeyType keyType) {
        return getStaticClass().getType();
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

    private BaseClass getBaseClass() {
        return classTables.get(0).getObjectSet().getBaseClass();
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return expr.translateQuery(translator).classExpr(classTables, type);
    }

    private static ImSet<ClassField> packTables(Where trueWhere, SingleClassExpr expr, ImSet<ClassField> tables, IsClassType type) {
        if(!type.isInconsistent() && tables.size() > 1) {
            final ValueClassSet exprClasses = IsClassWhere.getPackSet(trueWhere, expr);
            if(exprClasses != null)
                tables = tables.filterFn(new SFunctionSet<ClassField>() {
                public boolean contains(ClassField element) {
                    return !element.getObjectSet().and(exprClasses).isEmpty();
                }});
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

    public static boolean inSet(ConcreteObjectClass staticClass, ImSet<ClassField> classTables) {
        for(ClassField classTable : classTables)
            if(staticClass.inSet(classTable.getObjectSet()))
                return true;
        return false;
    }

    private ObjectValueClassSet getObjectSet() {
        return getBaseClass().getSet(classTables);
    }

    public Where calculateNotNullWhere() {
        return expr.isClass(getObjectSet(), type.isInconsistent());
    }

    protected int hash(HashContext hashContext) {
        return expr.hashOuter(hashContext) + type.hashCode();
    }

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return expr.equals(((IsClassExpr)obj).expr) && classTables.equals(((IsClassExpr)obj).classTables) && type.equals(((IsClassExpr)obj).type);
    }

    public Stat getStatValue() {
        return new Stat(getObjectSet().getClassCount());
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return new PropStat(getStatValue());
    }
    public InnerJoin<?, ?> getInnerJoin() {
        return getJoinExpr().getInnerJoin();
    }
    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>singleton(getJoinExpr());
    }

    // множественное наследование StaticClassExpr
    @Override
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return StaticClassExpr.getClassWhere(this, classes);
    }

    @Override
    public Expr classExpr(ImSet<ClassField> classes, IsClassType type) {
        return StaticClassExpr.classExpr(this, classes, type);
    }

    @Override
    public Where isClass(ValueClassSet set, boolean inconsistent) {
        return StaticClassExpr.isClass(this, set, inconsistent);
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
