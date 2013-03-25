package platform.server.data.where.classes;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.pull.ExclPullWheres;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.data.where.*;

public class ClassExprWhere extends AbstractClassWhere<VariableSingleClassExpr, ClassExprWhere> implements DNFWheres.Interface<ClassExprWhere>, OuterContext<ClassExprWhere>, KeyType {

    public Type getKeyType(KeyExpr keyExpr) {
        if (wheres.length == 0) {
            return ObjectType.instance;
        }
        AndClassSet classWhere = wheres[0].get(keyExpr);
        Type type;
        if(classWhere==null) {
            if(keyExpr instanceof PullExpr)
                return ObjectType.instance;
            else
                throw new RuntimeException("no classes");
        } else
            type = classWhere.getType();
        assert checkType(keyExpr,type);
        return type;
    }

    public Stat getKeyStat(KeyExpr keyExpr) {
        AndClassSet classSet = wheres[0].get(keyExpr);
        if(classSet==null) {
            if(keyExpr instanceof PullExpr)
                return Stat.ALOT;
            else
                throw new RuntimeException("no classes");
        } else
            return classSet.getTypeStat();
    }

    public Where getKeepWhere(KeyExpr keyExpr) {
        AndClassSet keepClass = null;
        for(And<VariableSingleClassExpr> where : wheres) {
            AndClassSet keyClass = where.getPartial(keyExpr);
            if (keyClass == null) // потому как в canbechanged например могут появляться pullExpr'ы без классов
                return Where.TRUE;
            AndClassSet keyKeepClass = keyClass.getKeepClass();
            if (keepClass == null)
                keepClass = keyKeepClass;
            else
                keepClass = keepClass.or(keyKeepClass);
        }

        return keyExpr.isClass(keepClass);
    }
    
    public boolean checkType(KeyExpr keyExpr,Type type) {
        for(int i=1;i<wheres.length;i++)
            assert type.getCompatible(wheres[0].get(keyExpr).getType())!=null;
        return true;
    }

    public Where getPackWhere() {
        if(isTrue()) return Where.TRUE;
        if(isFalse()) return Where.FALSE;
        return new PackClassWhere(this);
    }

    private ClassExprWhere(boolean isTrue) {
        super(isTrue);
    }

    public ClassExprWhere(And<VariableSingleClassExpr> where) {
        super(where);
    }

    public static ClassExprWhere TRUE = new ClassExprWhere(true);
    public static ClassExprWhere FALSE = new ClassExprWhere(false);

    protected ClassExprWhere FALSETHIS() {
        return FALSE;
    }

    public ClassExprWhere(VariableSingleClassExpr key, AndClassSet classes) {
        super(key,classes);
    }

    public ClassExprWhere(ImMap<KeyExpr,AndClassSet> andKeys) {
        this(new And<VariableSingleClassExpr>(andKeys));
    }

    public ClassExprWhere(ImMap<? extends VariableSingleClassExpr,ValueClass> andKeys, boolean up) {
        super((ImMap<VariableSingleClassExpr,ValueClass>) andKeys, true);
        assert up;
    }

    private ClassExprWhere(And<VariableSingleClassExpr>[] iWheres) {
        super(iWheres);
    }
    protected ClassExprWhere createThis(And<VariableSingleClassExpr>[] wheres) {
        return new ClassExprWhere(wheres);
    }

    private static And<VariableSingleClassExpr> andEquals(And<VariableSingleClassExpr> and, EqualMap equals) {
        MMap<VariableSingleClassExpr, AndClassSet> mResult = null;
        for(int i=0;i<equals.num;i++) {
            Equal equal = equals.comps[i];
            if(!equal.dropped) {
                AndClassSet andClasses = null;
                for(int j=0;j<equal.size;j++)
                    if(equal.exprs[j] instanceof VariableClassExpr) { // static'и особо не интересуют, так как либо в явную отработаны, либо не нужны (в булевой логике)
                        AndClassSet classes = equal.exprs[j].getAndClassSet(and);
                        if(classes!=null) {
                            if(andClasses==null)
                                andClasses = classes;
                            else {
                                andClasses = andClasses.and(classes);
                                if(andClasses.isEmpty()) return null;
                            }
                        }
                    }
                if(andClasses!=null)
                    for(int j=0;j<equal.size;j++)
                        if(equal.exprs[j] instanceof VariableSingleClassExpr) {
                            if(mResult==null)
                                mResult = MapFact.mMap(and, MapFact.<VariableSingleClassExpr, AndClassSet>override());
                            mResult.add((VariableSingleClassExpr) equal.exprs[j], andClasses);
                        }
            }
        }
        if(mResult==null)
            return and;
        else
            return new And<VariableSingleClassExpr>(mResult.immutable());
    }
    // нужен очень быстрый так как в checkTrue используется
    public ClassExprWhere andEquals(EqualMap equals) {
        if(equals.size()==0 || isFalse() || isTrue()) return this;

        And<VariableSingleClassExpr>[] rawAndWheres = newArray(wheres.length); int num=0;
        for(And<VariableSingleClassExpr> where : wheres) {
            And<VariableSingleClassExpr> andWhere = andEquals(where,equals);
            if(andWhere!=null)
                rawAndWheres[num++] = andWhere;
        }
        And<VariableSingleClassExpr>[] andWheres = newArray(num); System.arraycopy(rawAndWheres,0,andWheres,0,num);
        return new ClassExprWhere(andWheres);
    }

    private ClassExprWhere(ClassExprWhere classes, ImRevMap<VariableSingleClassExpr, VariableSingleClassExpr> map) {
        super(classes, map);
    }

    private <K> ImMap<K, AndClassSet> getFull(ImMap<K, ? extends BaseExpr> map) {
        MMap<K,AndClassSet> mResult = MapFact.mMap(AbstractClassWhere.<K>addOr());
        for(And<VariableSingleClassExpr> where : wheres)
            for(int i=0,size=map.size();i<size;i++) {
                AndClassSet classSet = map.getValue(i).getAndClassSet(where);
                if(classSet!=null)
                    mResult.add(map.getKey(i), classSet);
            }
        return mResult.immutable();
    }
    // получает классы для BaseExpr'ов
    public <K> ClassWhere<K> get(ImMap<K, ? extends BaseExpr> map, boolean full) {
        ImMap<K, AndClassSet> fullMap = null;
        if(full)
            fullMap = getFull(map);

        ClassWhere<K> transWhere = ClassWhere.FALSE();
        for(And<VariableSingleClassExpr> andWhere : wheres) {
            ImFilterValueMap<K, AndClassSet> andTrans = map.mapFilterValues();
            for(int i=0,size=map.size();i<size;i++) {
                AndClassSet classSet = map.getValue(i).getAndClassSet(andWhere);
                if(full && classSet==null)
                    classSet = fullMap.get(map.getKey(i));
                if(classSet!=null)
                    andTrans.mapValue(i, classSet);
            }
            transWhere = transWhere.or(new ClassWhere<K>(new And<K>(andTrans.immutableValue())));
        }
        return transWhere;
    }
    public <K> ClassWhere<K> map(ImRevMap<K, ? extends VariableSingleClassExpr> map) {
        ClassWhere<K> transWhere = ClassWhere.FALSE();
        for(And<VariableSingleClassExpr> andWhere : wheres)
            transWhere = transWhere.or(new ClassWhere<K>(andWhere.mapBack(map)));
        return transWhere;
    }

    public AndClassSet getAndClassSet(BaseExpr expr) {
        AndClassSet result = null;
        for(And<VariableSingleClassExpr> andWhere : wheres) {
            AndClassSet classSet = expr.getAndClassSet(andWhere);
            if(classSet!=null) {
                if(result == null)
                    result = classSet;
                else
                    result = result.or(classSet);
            }
        }
        return result;
    }

    public ImSet<NotNullExpr> getExprFollows() {
        ImSet<NotNullExpr>[] follows = new ImSet[wheres.length]; int num = 0;
        for(And<VariableSingleClassExpr> where : wheres) {
            MSet<NotNullExpr> mResult = SetFact.mSet();
            for(int i=0,size=where.size();i<size;i++)
                mResult.addAll(where.getKey(i).getExprFollows(true, true));
            follows[num++] = mResult.immutable();
        }
        return SetFact.and(follows);
    }

    // здесь не обязательно есть все BaseExpr'ы, равно как и то что они не повторяются
    public ClassExprWhere mapBack(ImMap<BaseExpr, ? extends BaseExpr> map) {
        int size = map.size();

        ClassExprWhere transWhere = ClassExprWhere.FALSE;
        for(And<VariableSingleClassExpr> andWhere : wheres) {
            boolean isFalse = false;
            MMap<VariableSingleClassExpr, AndClassSet> andTrans = MapFact.mMapMax(size, ClassExprWhere.<VariableSingleClassExpr>addAnd());
            for(int i=0;i<size;i++) {
                AndClassSet mapSet = map.getValue(i).getAndClassSet(andWhere);
                if(mapSet==null)
                    continue;
                if(!map.getKey(i).addAndClassSet(andTrans, mapSet)) {
                    isFalse = true;
                    break;
                }
            }
            if(!isFalse)
                transWhere = transWhere.or(new ClassExprWhere(new And<VariableSingleClassExpr>(andTrans.immutable())));
        }
        return transWhere;
    }

    // assert reversed, where содержит groups
    public static ClassExprWhere mapBack(ImMap<BaseExpr, ? extends Expr> outerInner, Where innerWhere) {
        return new ExclPullWheres<ClassExprWhere, BaseExpr, Where>() {
            protected ClassExprWhere initEmpty() {
                return ClassExprWhere.FALSE;
            }
            protected ClassExprWhere proceedBase(Where data, ImMap<BaseExpr, BaseExpr> outerInner) {
                return data.getClassWhere().mapBack(outerInner);
            }
            protected ClassExprWhere add(ClassExprWhere op1, ClassExprWhere op2) {
                return op1.or(op2);
            }
        }.proceed(innerWhere, outerInner);
    }

    // assert reversed, where не содержит groups
    public static ClassExprWhere mapBack(Where where, final ImRevMap<BaseExpr, Expr> outerInner) {
        final Where outerWhere = where.and(Expr.getWhere(outerInner.keys()));
        return new ExclPullWheres<ClassExprWhere, BaseExpr, Where>() {
            protected ClassExprWhere initEmpty() {
                return ClassExprWhere.FALSE;
            }
            protected ClassExprWhere proceedBase(Where data, ImMap<BaseExpr, BaseExpr> outerInner) {
                Result<ImRevMap<BaseExpr, BaseExpr>> innerOuter = new Result<ImRevMap<BaseExpr, BaseExpr>>();
                Where where = outerWhere.and(GroupExpr.getEqualsWhere(GroupExpr.groupMap(outerInner, outerWhere.getExprValues(), innerOuter)));
                return where.getClassWhere().mapBack(innerOuter.result).and(data.getClassWhere());
            }
            protected ClassExprWhere add(ClassExprWhere op1, ClassExprWhere op2) {
                return op1.or(op2);
            }
        }.proceed(Expr.getWhere(outerInner.values()), outerInner);
    }

    private class OuterContext extends AbstractOuterContext<OuterContext> {

        protected OuterContext translate(MapTranslate translator) {
            return new ClassExprWhere(ClassExprWhere.this, translator.translateVariable(keySet().toRevMap())).getOuter();
        }

        protected int hash(HashContext hash) {
            int result = 0;
            for(And<VariableSingleClassExpr> andWhere : wheres)
                result += AbstractOuterContext.hashKeysOuter(andWhere, hash);
            return result;
        }

        public ImSet<platform.server.caches.OuterContext> calculateOuterDepends() {
            return BaseUtils.immutableCast(keySet());
        }

        protected boolean isComplex() {
            return true;
        }

        private ClassExprWhere getThis() {
            return ClassExprWhere.this;
        }

        public boolean twins(TwinImmutableObject o) {
            return getThis().equals(((OuterContext)o).getThis());
        }
    }
    private OuterContext outer;
    private OuterContext getOuter() {
        if(outer==null)
            outer = new OuterContext();
        return outer;
    }
    public ImSet<KeyExpr> getOuterKeys() {
        return getOuter().getOuterKeys();
    }
    public ImSet<Value> getOuterValues() {
        return getOuter().getOuterValues();
    }
    public int hashOuter(HashContext hashContext) {
        return getOuter().hashOuter(hashContext);
    }
    public ImSet<platform.server.caches.OuterContext> getOuterDepends() {
        return getOuter().getOuterDepends();
    }
    public boolean enumerate(ExprEnumerator enumerator) {
        return getOuter().enumerate(enumerator);
    }
    public long getComplexity(boolean outer) {
        return getOuter().getComplexity(outer);
    }
    public ClassExprWhere translateOuter(MapTranslate translator) {
        return getOuter().translateOuter(translator).getThis();
    }
    public ClassExprWhere pack() {
        throw new RuntimeException("not supported yet");
    }

    // аналогичный метод в ClassWhere
    public ClassExprWhere remove(ImSet<? extends VariableSingleClassExpr> keys) {
        ClassExprWhere result = ClassExprWhere.FALSE;
        for(And<VariableSingleClassExpr> andWhere : wheres)
            result = result.or(new ClassExprWhere(andWhere.remove(keys)));
        return result;
    }

    public ClassExprWhere filterInclKeys(ImSet<? extends VariableSingleClassExpr> keys) {
        ClassExprWhere result = ClassExprWhere.FALSE;
        for(And<VariableSingleClassExpr> andWhere : wheres)
            result = result.or(new ClassExprWhere((And<VariableSingleClassExpr>) andWhere.filterInclKeys(keys)));
        return result;
    }

}
