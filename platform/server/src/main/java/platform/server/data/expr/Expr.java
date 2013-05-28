package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.SFunctionSet;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.classes.*;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.formula.FormulaExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.cases.ExprCase;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.expr.where.cases.MExprCaseList;
import platform.server.data.expr.where.ifs.IfExpr;
import platform.server.data.expr.where.ifs.MIfCases;
import platform.server.data.expr.where.ifs.NullExpr;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.Query;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.ClassReader;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassField;

import java.sql.SQLException;
import java.util.Set;

// абстрактный класс выражений

abstract public class Expr extends AbstractSourceJoin<Expr> {

    public static final boolean useCases = false;
    public static final Expr NULL = useCases?new CaseExpr(new ExprCaseList(SetFact.<ExprCase>EMPTY())):NullExpr.instance;
    public static CaseExprInterface newCases(boolean exclusive) {
        if(useCases || exclusive)
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
        return classExpr(baseClass.getUpTables().keys());
    }
    public abstract Expr classExpr(ImSet<ClassField> classes); // classes - за пределами которых можно (и нужно ?) возвращать null

    public abstract Where isClass(ValueClassSet set);
    public Where isUpClass(ValueClass set) {
        return isClass(set.getUpSet());
    }

    public boolean isNull() {
        return getWhere().isFalse();
    }

    public abstract Where compareBase(BaseExpr expr, Compare compareBack);
    public abstract Where compare(Expr expr, Compare compare);
    public Where compare(Expr expr, boolean min) {
        return compare(expr, Compare.get(min));
    }

    public Where compare(DataObject data, Compare compare) {
        return compare(data.getExpr(),compare);
    }

//    public abstract Expr scale(int coeff);

//    public abstract Expr sum(Expr expr);
    public Expr scale(int coeff) {
        if(coeff==1) return this;

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

        return ifElse(where, Expr.NULL);
    }
    public Expr ifElse(Where where, Expr elseExpr) {
        if(Expr.useCases) {
            MExprCaseList mCases = new MExprCaseList(false);
            mCases.add(where,this);
            mCases.add(Where.TRUE,elseExpr);
            return mCases.getFinal();
        } else
            return IfExpr.create(where, this, elseExpr);
    }
    public Expr compareExpr(Expr expr, boolean min) {
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

    public abstract Expr translateQuery(QueryTranslator translator);

    public static Where getWhere(ImList<? extends Expr> list) {
        return getWhere(list.getCol());
    }
    
    public static Where getWhere(ImCol<? extends Expr> col) {
        Where where = Where.TRUE;
        for(Expr expr : col)
            where = where.and(expr.getWhere());
        return where;
    }

    public static Where getOrWhere(ImCol<? extends Expr> col) {
        Where where = Where.FALSE;
        for(Expr expr : col)
            where = where.or(expr.getWhere());
        return where;
    }

    public static <K> Where getWhere(ImMap<K, ? extends Expr> map) {
        return getWhere(map.values());
    }

    public void checkInfiniteKeys() {
        ImSet<KeyExpr> keys = BaseUtils.immutableCast(getOuterKeys());

        Result<ImSet<KeyExpr>> keyRest = new Result<ImSet<KeyExpr>>();
        ImSet<KeyExpr> keyValues = keys.split(new SFunctionSet<KeyExpr>() {
            public boolean contains(KeyExpr key) {
                return key.getType(getWhere()) instanceof DataClass;
            }
        }, keyRest);

        new Query<KeyExpr,Object>(keyRest.result.toRevMap(),
                     translateQuery(new PartialQueryTranslator(keyValues.mapValues(new GetValue<Expr, KeyExpr>() {
                         public Expr getMapValue(KeyExpr key) {
                             return ((DataClass)key.getType(getWhere())).getDefaultExpr();
                         }}), true)).getWhere()).compile(PostgreDataAdapter.debugSyntax);
    }

    public static <K> ImMap<K, ObjectValue> readValues(SQLSession session, BaseClass baseClass, ImMap<K,Expr> mapExprs, QueryEnvironment env) throws SQLException { // assert что в mapExprs только values
        MExclMap<K, ObjectValue> mMapValues = MapFact.<K, ObjectValue>mExclMap(mapExprs.size());
        MExclMap<K, Expr> mMapExprValues = MapFact.<K, Expr>mExclMap(mapExprs.size());
        for(int i=0,size=mapExprs.size();i<size;i++) {
            Expr expr = mapExprs.getValue(i);
            ObjectValue objectValue = expr.getObjectValue();
            if(objectValue!=null)
                mMapValues.exclAdd(mapExprs.getKey(i), objectValue);
            else
                mMapExprValues.exclAdd(mapExprs.getKey(i), expr);
        }
        ImMap<K, ObjectValue> mapValues = mMapValues.immutable();
        ImMap<K, Expr> mapExprValues = mMapExprValues.immutable();

        if(mapExprValues.isEmpty()) // чисто для оптимизации чтобы лишний раз executeClasses не вызывать
            return mapValues;
        else
            return mapValues.addExcl(new Query<Object, K>(MapFact.<Object, KeyExpr>EMPTYREV(), mapExprValues, Where.TRUE).executeClasses(session, env, baseClass).singleValue());
    }

    public abstract Where getBaseWhere();

    public abstract Stat getTypeStat(Where fullWhere);

    public abstract Set<BaseExpr> getBaseExprs();
    
    public ObjectValue getObjectValue() {
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

