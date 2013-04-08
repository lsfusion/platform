package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.SFunctionSet;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.OuterContext;
import platform.server.caches.ParamLazy;
import platform.server.caches.TwinLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.*;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.PropStat;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.query.SubQueryExpr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.expr.where.extra.IsClassWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.AbstractWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.logics.property.ClassField;

public class IsClassExpr extends InnerExpr implements StaticClassExprInterface {

    public final SingleClassExpr expr;
    private final ImSet<ClassField> classTables;

    public IsClassExpr(SingleClassExpr expr, ImSet<ClassField> classTables) {
        this.expr = expr;

        this.classTables = classTables;
    }

    public static final int subqueryThreshold = 6;
    @TwinLazy
    public InnerExpr getJoinExpr() {
        if(classTables.size()==1) {
            return classTables.single().getStoredExpr(expr);
        } else {
            KeyExpr keyExpr = new KeyExpr("isSetClass");
            assert classTables.size() > inlineThreshold;
            // будем делить на inlineThreshold корзин
            Pair<KeyExpr,Expr> subQuery = getBaseClass().getSubQuery(classTables);
            return new SubQueryExpr(subQuery.second, MapFact.singleton(subQuery.first, (BaseExpr) expr));
        }
    }

    public static final int inlineThreshold = 4;
    public static Expr create(SingleClassExpr expr, ImSet<ClassField> classes) {
        classes = packTables(Where.TRUE, expr, classes);

        if(classes.size()> inlineThreshold || classes.size()==1)
            return new IsClassExpr(expr, classes);
        else
            return getTableExpr(expr, classes, inlineThreshold);
    }

    public static Expr getTableExpr(SingleClassExpr expr, ImSet<ClassField> classTables, final int threshold) {
        final ImOrderSet<ClassField> orderClassTables = classTables.toOrderSet();
        CaseExprInterface mCases = Expr.newCases(true);

        ImMap<Integer, ImSet<ClassField>> group = classTables.group(new BaseUtils.Group<Integer, ClassField>() {
            public Integer group(ClassField key) {
                return orderClassTables.indexOf(key) % threshold;
            }});

//        IsClassWhere[] classWheres = new IsClassWhere[group.size()];
        for(int i=0,size=group.size();i<size;i++) {
            Expr classExpr = create(expr, group.get(i));
            if(classTables.size()==1) // оптимизация и разрыв рекурсии
                return classExpr;
            Where where = classExpr.getWhere();
            mCases.add(where, classExpr);

//            classWheres[i] = where;
        }
        Expr result = mCases.getFinal();

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
    public Stat getTypeStat(KeyStat keyStat) {
        return getStaticClass().getTypeStat();
    }

    public ConcreteCustomClass getStaticClass() {
        return getBaseClass().objectClass;
    }

    private BaseClass getBaseClass() {
        return classTables.get(0).getSet().getBaseClass();
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return expr.translateQuery(translator).classExpr(classTables);
    }

    private static ImSet<ClassField> packTables(Where trueWhere, SingleClassExpr expr, ImSet<ClassField> tables) {
        if(tables.size() > 1) {
            final ValueClassSet exprClasses = IsClassWhere.getPackSet(trueWhere, expr);
            if(exprClasses != null)
                tables = tables.filterFn(new SFunctionSet<ClassField>() {
                public boolean contains(ClassField element) {
                    return !element.getSet().and(exprClasses).isEmpty();
                }});
        }
        return tables;
    }
    @Override
    public Expr packFollowFalse(Where where) {
        return expr.packFollowFalse(where).classExpr(packTables(where.not(), expr, classTables)); // два раза будет паковаться, но может и правильно потому как expr меняется
    }
    protected IsClassExpr translate(MapTranslate translator) {
        return new IsClassExpr(expr.translateOuter(translator), classTables);
    }

    public static boolean inSet(ConcreteObjectClass staticClass, ImSet<ClassField> classTables) {
        for(ClassField classTable : classTables)
            if(staticClass.inSet(classTable.getSet()))
                return true;
        return false;
    }

    private ObjectValueClassSet getSet() {
        return getBaseClass().getSet(classTables);
    }

    public Where calculateNotNullWhere() {
        return expr.isClass(getSet());
    }

    protected int hash(HashContext hashContext) {
        return expr.hashOuter(hashContext)+1;
    }

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    public boolean twins(TwinImmutableObject obj) {
        return expr.equals(((IsClassExpr)obj).expr) && classTables.equals(((IsClassExpr)obj).classTables);
    }

    public Stat getStatValue() {
        return new Stat(getSet().getClassCount());
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
    public Expr classExpr(ImSet<ClassField> classes) {
        return StaticClassExpr.classExpr(this, classes);
    }

    @Override
    public Where isClass(ValueClassSet set) {
        return StaticClassExpr.isClass(this, set);
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
