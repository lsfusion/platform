package platform.server.data.expr;

import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.OuterContext;
import platform.server.caches.ParamLazy;
import platform.server.caches.TwinLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.Table;
import platform.server.data.expr.query.PropStat;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.CompileSource;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

public class IsClassExpr extends InnerExpr implements StaticClassExprInterface {

    public final SingleClassExpr expr;
    final BaseClass baseClass;

    public IsClassExpr(SingleClassExpr expr, BaseClass baseClass) {
        this.expr = expr;
        
        this.baseClass = baseClass;
    }

    @TwinLazy
    public Table.Join.Expr getJoinExpr() {
        return baseClass.getJoinExpr(expr);
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
        return baseClass.objectClass;
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return expr.translateQuery(translator).classExpr(baseClass);
    }
    @Override
    public Expr packFollowFalse(Where where) {
        return expr.packFollowFalse(where).classExpr(baseClass);
    }
    protected IsClassExpr translate(MapTranslate translator) {
        return new IsClassExpr(expr.translateOuter(translator),baseClass);
    }

    public Where calculateNotNullWhere() {
        return expr.isClass(baseClass.getUpSet());
    }

    protected int hash(HashContext hashContext) {
        return expr.hashOuter(hashContext)+1;
    }

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    public boolean twins(TwinImmutableObject obj) {
        return expr.equals(((IsClassExpr)obj).expr) && baseClass.equals(((IsClassExpr)obj).baseClass);
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return new PropStat(new Stat(getStaticClass().getCount()));
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
    public Expr classExpr(BaseClass baseClass) {
        return StaticClassExpr.classExpr(this, baseClass);
    }

    @Override
    public Where isClass(AndClassSet set) {
        return StaticClassExpr.isClass(this, set);
    }

    @Override
    public AndClassSet getAndClassSet(ImMap<VariableClassExpr, AndClassSet> and) {
        return StaticClassExpr.getAndClassSet(this, and);
    }

    @Override
    public boolean addAndClassSet(MMap<VariableClassExpr, AndClassSet> and, AndClassSet add) {
        return StaticClassExpr.addAndClassSet(this, add);
    }

}
