package lsfusion.server.data.expr;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.AbstractSourceJoin;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.formula.FormulaUnionExpr;
import lsfusion.server.data.expr.formula.MLinearOperandMap;
import lsfusion.server.data.expr.formula.MaxFormulaImpl;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.expr.where.cases.ExprCase;
import lsfusion.server.data.expr.where.cases.ExprCaseList;
import lsfusion.server.data.expr.where.cases.MExprCaseList;
import lsfusion.server.data.expr.where.ifs.IfExpr;
import lsfusion.server.data.expr.where.ifs.MIfCases;
import lsfusion.server.data.expr.where.ifs.NullExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.compile.CompileOptions;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.PartialKeyExprTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.reader.ClassReader;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;

import java.sql.SQLException;
import java.util.function.Function;

// абстрактный класс выражений

abstract public class Expr extends AbstractSourceJoin<Expr> {

    public static int useCasesCount = Integer.MAX_VALUE;

    // converted to static method to prevent class initialization deadlocks 
    public static Expr NULL() {
        return NULL;
    }
    
    private static final Expr NULL = (useCasesCount == 0 ? new CaseExpr(new ExprCaseList(SetFact.EMPTY())) : NullExpr.instance);
    
    public static CaseExprInterface newCases(boolean exclusive, int size) {
        if(size >= useCasesCount || exclusive)
            return new MExprCaseList(exclusive);
        else
            return new MIfCases();
    }

    public abstract Type getType(KeyType keyType);
    @IdentityLazy
    public Type getSelfType() {
        return getType(getWhere());
    }   

    public abstract ClassReader getReader(KeyType keyType);
    
    public abstract String getSource(CompileSource source, boolean needValue);
    public String getSource(CompileSource source) {
        return getSource(source, true);
    }
    public String getNullSource(CompileSource source) {
        return getSource(source, false) + " IS NULL";
    }
    public String getNotNullSource(CompileSource source) {
        return getSource(source, false) + " IS NOT NULL";
    }

    @Override
    public String toString() { // it's important that it should be overrided for all getOuterValues objects (AbstractValueExpr), and for all classes for which in ToString.getSource toString is called (KeyExpr, Table.Join.Expr, QueryExpr)
        return getSource(new ToString(getOuterValues()));
    }

    public abstract int getWhereDepth();

    // возвращает Where на notNull
    private Where where=null;
    @ManualLazy
    public Where getWhere() {
        if(where==null)
            where = calculateWhere();
        return where;
    }
/*    public void setWhere(Where setWhere) {
        where = setWhere;
    }*/
    public abstract Where calculateWhere();

    // получает список ExprCase'ов
    public abstract ExprCaseList getCases();

    public Expr classExpr(BaseClass baseClass) {
        return classExpr(baseClass, IsClassType.CONSISTENT);
    }
    public Expr classExpr(BaseClass baseClass, IsClassType type) {
        return classExpr(baseClass.getUpObjectClassFields().keys(), type);
    }
    public Expr classExpr(ObjectValueClassSet valueClassSet, IsClassType type) {
        assert !valueClassSet.isEmpty();
        return classExpr(valueClassSet.getObjectClassFields().keys(), type);
    }
    public abstract Expr classExpr(ImSet<ObjectClassField> classes, IsClassType type); // classes - за пределами которых можно (и нужно ?) возвращать null

    @IdentityLazy
    public Expr classExpr(ObjectClassField field) {
        return classExpr(SetFact.singleton(field), IsClassType.CONSISTENT);
    }

    public Where isClass(ValueClassSet set) {
        return isClass(set, IsClassType.CONSISTENT);
    }
    public abstract Where isClass(ValueClassSet set, IsClassType type);
    public Where isUpClass(ValueClass set) {
        return isUpClass(set, IsClassType.CONSISTENT);
    }

    public Where isUpClass(ValueClass set, IsClassType type) {
        return isClass(set.getUpSet(), type);
    }

    public boolean isNull() {
        return getWhere().isFalse();
    }

    public boolean isTrue() {
        return getWhere().isTrue();
    }

    // оптимизация, нужна чтобы когда не нужно конкретное значение, GROUP SUM преобразовывать в GROUP ANY без боязни в результате SUM получить 0, а значит NULL, во общем то пока оптимизация для GROUP SUM 1 и других COUNT'ов (потом при появлении POSITIVE constraint'ов может можно в более общем случае использовать)
    public abstract boolean isAlwaysPositiveOrNull();

    public abstract Where compareBase(BaseExpr expr, Compare compareBack);
    public abstract Where compare(Expr expr, Compare compare);
    public Where compare(Expr expr, boolean min) {
        return compare(expr, Compare.get(min));
    }
    public Where equalsNull(Expr expr) {
        return compare(expr, Compare.EQUALS).or(getWhere().not().and(expr.getWhere().not()));
    }
    
/*    public Where equalsFull(Expr expr) {
        return compare(expr, Compare.EQUALS).or(getWhere().not().and(expr.getWhere().not()));
    }*/

    public Where compare(DataObject data, Compare compare) {
        return compare(data.getExpr(),compare);
    }

//    public abstract Expr scale(int coeff);

//    public abstract Expr sum(Expr expr);
    public Expr scale(int coeff) {
        // нельзя делать эту оптимизацию так как идет проверка на 0 в логике
//        if(coeff==1) return this;

        MLinearOperandMap map = new MLinearOperandMap();
        map.add(this,coeff);
        return map.getExpr();
    }
    
    public Expr mult(Expr expr, IntegralClass intClass) {
        return FormulaExpr.createCustomFormula(FormulaExpr.MULT2, intClass, this, expr);
    }

    public Expr sum(Expr expr) {
        // нельзя делать эту оптимизацию так как идет проверка на 0 в логике
//        if(getWhere().means(expr.getWhere().not())) // если не пересекаются то возвращаем case
//            return nvl(expr);

        MLinearOperandMap map = new MLinearOperandMap();
        map.add(this,1);
        map.add(expr,1);
        return map.getExpr();
    }

    public Expr diff(Expr expr) {
        return sum(expr.scale(-1));
    }

    public Expr and(Where where) {
        if(getWhere().means(where))
            return this;

        return ifElse(where, Expr.NULL());
    }
    public Expr ifElse(Where where, Expr elseExpr) {
        if(Expr.useCasesCount <= 2) {
            MExprCaseList mCases = new MExprCaseList(false);
            mCases.add(where,this);
            mCases.add(Where.TRUE(),elseExpr);
            return mCases.getFinal();
        } else
            return IfExpr.create(where, this, elseExpr);
    }
    public Expr compareExpr(Expr expr, boolean min) {
        return FormulaUnionExpr.create(new MaxFormulaImpl(min), ListFact.toList(this, expr));
    }
    public Expr calcCompareExpr(Expr expr, boolean min) {
        return ifElse(compare(expr, min).or(expr.getWhere().not()),expr);
    }

    public Expr max(Expr expr) {
        return compareExpr(expr, false);
    }
    public Expr min(Expr expr) {
        return compareExpr(expr, true);
    }
    public Expr nvl(Expr expr) {
        return ifElse(getWhere(),expr);
    }

    public Expr translateExpr(ExprTranslator translator) {
        return super.translateExpr(translator);
    }

    public static Where getWhere(ImList<? extends Expr> list) {
        return getWhere(list.getCol());
    }
    
    public static Where getWhere(ImCol<? extends Expr> col) {
        Where where = Where.TRUE();
        for(Expr expr : col)
            where = where.and(expr.getWhere());
        return where;
    }

    public static Where getOrWhere(ImCol<? extends Expr> col) {
        Where where = Where.FALSE();
        for(Expr expr : col)
            where = where.or(expr.getWhere());
        return where;
    }

    public static <K> Where getWhere(ImMap<K, ? extends Expr> map) {
        return getWhere(map.values());
    }

    public void checkInfiniteKeys() {
        ImSet<KeyExpr> keys = BaseUtils.immutableCast(getOuterKeys());

        Result<ImSet<KeyExpr>> keyRest = new Result<>();
        ImSet<KeyExpr> keyValues = keys.split(key -> key.getType(getWhere()) instanceof DataClass, keyRest);

        new Query<>(keyRest.result.toRevMap(),
                translateExpr(new PartialKeyExprTranslator(keyValues.mapValues(new Function<KeyExpr, Expr>() {
                    public Expr apply(KeyExpr key) {
                        return ((DataClass) key.getType(getWhere())).getDefaultExpr();
                    }
                }), true)).getWhere()).compile(new CompileOptions<>(DataAdapter.debugSyntax));
    }

    public static Object readValue(SQLSession session, Expr expr, OperationOwner owner) throws SQLException, SQLHandledException { // assert что в mapExprs только values
        return new Query<>(MapFact.EMPTYREV(), MapFact.singleton("value", expr), Where.TRUE()).execute(session, owner).singleValue().singleValue();
    }

    public static ObjectValue readObjectValue(SQLSession session, BaseClass baseClass, Expr expr, QueryEnvironment env) throws SQLException, SQLHandledException { // assert что в mapExprs только values
        ObjectValue objectValue = expr.getObjectValue(env);
        if(objectValue != null)
            return objectValue;
        return new Query<>(MapFact.EMPTYREV(), MapFact.singleton("value", expr), Where.TRUE()).executeClasses(session, env, baseClass).singleValue().singleValue(); 
    }

    public static <K> ImMap<K, Object> readValues(SQLSession session, ImMap<K,Expr> mapExprs, OperationOwner owner) throws SQLException, SQLHandledException { // assert что в mapExprs только values
        return new Query<>(MapFact.EMPTYREV(), mapExprs, Where.TRUE()).execute(session, owner).singleValue();
    }
    
    public static <K> ImMap<K, ObjectValue> readObjectValues(SQLSession session, BaseClass baseClass, ImMap<K,Expr> mapExprs, QueryEnvironment env) throws SQLException, SQLHandledException { // assert что в mapExprs только values
        MExclMap<K, ObjectValue> mMapValues = MapFact.mExclMapMax(mapExprs.size());
        MExclMap<K, Expr> mMapExprValues = MapFact.mExclMapMax(mapExprs.size());
        for(int i=0,size=mapExprs.size();i<size;i++) {
            Expr expr = mapExprs.getValue(i);
            ObjectValue objectValue = expr.getObjectValue(env);
            if(objectValue != null)
                mMapValues.exclAdd(mapExprs.getKey(i), objectValue);
            else
                mMapExprValues.exclAdd(mapExprs.getKey(i), expr);
        }
        ImMap<K, ObjectValue> mapValues = mMapValues.immutable();
        ImMap<K, Expr> mapExprValues = mMapExprValues.immutable();

        if(mapExprValues.isEmpty())
            return mapValues;
        else
            return mapValues.addExcl(new Query<>(MapFact.EMPTYREV(), mapExprValues, Where.TRUE()).executeClasses(session, env, baseClass).singleValue());
    }

    public static <K> ImMap<K, ObjectValue> getObjectValues(ImMap<K,Expr> mapExprs, QueryEnvironment env) {
        MExclMap<K, ObjectValue> mMapValues = MapFact.mExclMap(mapExprs.size());
        for(int i=0,size=mapExprs.size();i<size;i++) {
            Expr expr = mapExprs.getValue(i);
            ObjectValue objectValue = expr.getObjectValue(env);
            if(objectValue != null)
                mMapValues.exclAdd(mapExprs.getKey(i), objectValue);
            else
                return null;
        }
        return mMapValues.immutable();
    }

    public abstract Where getBaseWhere();

    public abstract Stat getTypeStat(Where fullWhere, boolean forJoin);

    public abstract ImSet<BaseExpr> getBaseExprs();
    
    public ObjectValue getObjectValue(QueryEnvironment env) {
        return null;
    }

    public static Where andExprCheck(Where where1, Where where2) {
//        return where1.and(where2);
        return (Where) where1.andCheck(where2);
    }
    
    public static Where orExprCheck(Where where1, Where where2) {
//        return where1.or(where2);
        return (Where) where1.orCheck(where2);
    }

    public abstract ConcreteClass getStaticClass(); // вообще можно будет убрать, потом оставить у StaticClassExpr - очень мелкая оптимизация
}

