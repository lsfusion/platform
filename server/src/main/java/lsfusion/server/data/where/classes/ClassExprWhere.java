package lsfusion.server.data.where.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.ContextEnumerator;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.classes.VariableClassExpr;
import lsfusion.server.data.expr.classes.VariableSingleClassExpr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.expr.where.pull.ExclPullWheres;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.Equal;
import lsfusion.server.data.where.EqualMap;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;

public class ClassExprWhere extends AbstractClassWhere<VariableSingleClassExpr, ClassExprWhere> implements DNFWheres.Interface<ClassExprWhere>, OuterContext<ClassExprWhere>, KeyType {

    public Type getKeyType(ParamExpr keyExpr) {
        if (wheres.length == 0) {
            if(keyExpr instanceof PullExpr)
                return ObjectType.instance;
            return null;
        }
        Type result = null;
        for(And<VariableSingleClassExpr> where : wheres) {
            AndClassSet classWhere = where.get(keyExpr);
            if(classWhere==null) {
                if(keyExpr instanceof PullExpr)
                    return ObjectType.instance;
                return null;
//                else
//                    throw new RuntimeException("no classes"); 
            }
            Type whereType = classWhere.getType();
            if(result == null)
                result = whereType;
            else
                result = result.getCompatible(whereType);
        }

        // если null, см. BaseExpr.pushValues или Settings.limitClassWhereCount
        return result;
    }

    public Stat getKeyStat(ParamExpr keyExpr, boolean forJoin) {
        AndClassSet classSet = wheres[0].get(keyExpr);
        if(classSet==null) {
            //if(keyExpr instanceof PullExpr)
                return Stat.ALOT;
            //else
            //    throw new RuntimeException("no classes"); // см. ClassExprWhere.getKeyType
        } else
            return classSet.getType().getTypeStat(forJoin);
    }

    public Where getKeepWhere(KeyExpr keyExpr, boolean noInnerFollows) {
        ValueClassSet keepClass = null;
        for(And<VariableSingleClassExpr> where : wheres) {
            AndClassSet keyClass = where.getPartial(keyExpr);
            if (keyClass == null || !BaseUtils.hashEquals(keyClass, keyClass.getValueClassSet())) // потому как в canbechanged например могут появляться pullExpr'ы без классов, если Unknown то нет смысла сохранять, все равно класс не дает
                return Where.TRUE();
            ValueClassSet valueKeyClass = (ValueClassSet) keyClass;
            ValueClassSet keyKeepClass = valueKeyClass;// .getKeepClass();
            if (keepClass == null)
                keepClass = keyKeepClass;
            else
                keepClass = (ValueClassSet)keepClass.or(keyKeepClass);
        }
        
        return keyExpr.isClass(keepClass, noInnerFollows ? IsClassType.VIRTUAL : IsClassType.CONSISTENT);
    }
    
    public boolean checkType(ParamExpr keyExpr,Type type) {
        for(int i=1;i<wheres.length;i++)
            assert type.getCompatible(wheres[0].get(keyExpr).getType())!=null;
        return true;
    }

    public Where getPackWhere() {
        if(isTrue()) return Where.TRUE();
        if(isFalse()) return Where.FALSE();
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

    private static And<VariableSingleClassExpr> andEquals(And<VariableSingleClassExpr> and, EqualMap equals, boolean onlyData) {
        MMap<VariableSingleClassExpr, AndClassSet> mResult = null;
        for(int i=0;i<equals.num;i++) {
            Equal equal = equals.comps[i];
            if(!equal.dropped) {
                boolean[] noClasses = null;
                if(onlyData)
                    noClasses = new boolean[equal.size];
                AndClassSet andClasses = null;
                for(int j=0;j<equal.size;j++)
                    if(equal.exprs[j] instanceof VariableClassExpr) { // static'и особо не интересуют, так как либо в явную отработаны, либо не нужны (в булевой логике)
                        AndClassSet classes = equal.exprs[j].getAndClassSet(and);
                        if(classes!=null) {
                            if(onlyData && !(classes instanceof DataClass))
                                break;

                            if(andClasses==null)
                                andClasses = classes;
                            else {
                                andClasses = andClasses.and(classes);
                                if(andClasses.isEmpty()) return null;
                            }
                        } else {
                            if(onlyData)
                                noClasses[j] = true;
                        }
                    }
                if(andClasses!=null)
                    for(int j=0;j<equal.size;j++)
                        if(equal.exprs[j] instanceof VariableSingleClassExpr && (!onlyData || noClasses[j])) { // выводим класс только если его не было (наиболее логичное поведение для сравнений)
                            if(mResult==null)
                                mResult = MapFact.mMap(and, MapFact.<VariableSingleClassExpr, AndClassSet>override());
                            mResult.add((VariableSingleClassExpr) equal.exprs[j], andClasses);
                        }
            }
        }
        if(mResult==null)
            return and;
        else
            return new And<>(mResult.immutable());
    }

    public ClassExprWhere andEquals(EqualMap equals) {
        return andEquals(equals, false);
    }
    // нужен очень быстрый так как в checkTrue используется
    public ClassExprWhere andEquals(EqualMap equals, boolean onlyData) {
        if(equals.size()==0 || isFalse()) return this; // || isTrue() is incorrect check since we may loose equal operands classes

        And<VariableSingleClassExpr>[] rawAndWheres = newArray(wheres.length); int num=0;
        for(And<VariableSingleClassExpr> where : wheres) {
            And<VariableSingleClassExpr> andWhere = andEquals(where,equals,onlyData);
            if(andWhere!=null)
                rawAndWheres[num++] = andWhere;
        }
        And<VariableSingleClassExpr>[] andWheres = newArray(num); System.arraycopy(rawAndWheres,0,andWheres,0,num);
        return new ClassExprWhere(andWheres);
    }

    private ClassExprWhere(ClassExprWhere classes, ImRevMap<VariableSingleClassExpr, VariableSingleClassExpr> map) {
        super(classes, map);
    }

    // получает классы для BaseExpr'ов
    public <K> ClassWhere<K> get(ImMap<K, ? extends BaseExpr> map) {
        ClassWhere<K> transWhere = ClassWhere.FALSE();
        for(And<VariableSingleClassExpr> andWhere : wheres) {
            ImFilterValueMap<K, AndClassSet> andTrans = map.mapFilterValues();
            for(int i=0,size=map.size();i<size;i++) {
                AndClassSet classSet = map.getValue(i).getAndClassSet(andWhere);
                if(classSet!=null)
                    andTrans.mapValue(i, classSet);
            }
            transWhere = transWhere.or(new ClassWhere<>(new And<>(andTrans.immutableValue())));
        }
        return transWhere;
    }
    public <K> ClassWhere<K> mapClasses(ImRevMap<K, ? extends VariableSingleClassExpr> map) {
        ClassWhere<K> transWhere = ClassWhere.FALSE();
        for(And<VariableSingleClassExpr> andWhere : wheres)
            transWhere = transWhere.or(new ClassWhere<>(andWhere.mapBack(map)));
        return transWhere;
    }

    public AndClassSet getAndClassSet(BaseExpr expr) {
        AndClassSet result = null;
        for(And<VariableSingleClassExpr> andWhere : wheres) {
            AndClassSet classSet = expr.getAndClassSet(andWhere);
            if(classSet == null) // any class
                break;

            if(result == null)
                result = classSet;
            else
                result = result.or(classSet);
        }
        return result;
    }

    public ImSet<NullableExprInterface> getExprFollows() {
        ImSet<NullableExprInterface>[] follows = new ImSet[wheres.length]; int num = 0;
        for(And<VariableSingleClassExpr> where : wheres) {
            MSet<NullableExprInterface> mResult = SetFact.mSet();
            for(int i=0,size=where.size();i<size;i++)
                mResult.addAll(where.getKey(i).getExprFollows(true, NullableExpr.FOLLOW, true));
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
                if(mapSet != null) {
                    if (!map.getKey(i).addAndClassSet(andTrans, mapSet)) {
                        isFalse = true;
                        break;
                    }
                }
            }
            if(!isFalse)
                transWhere = transWhere.or(new ClassExprWhere(new And<>(andTrans.immutable())));
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
                // тут фокус в том что так-как exclusive в CaseList не гарантируется с точки зрения булевой логики (только с точки зрения семантики), getWhere за пределами inner не гарантирует все assertion'ы (например то что SingleClassExpr.isClass - getOrSet не null + assertion в intersect)
                return data.and(Expr.getWhere(outerInner)).getClassWhere().mapBack(outerInner);
            }
            protected ClassExprWhere add(ClassExprWhere op1, ClassExprWhere op2) {
                return op1.or(op2);
            }
        }.proceed(innerWhere, outerInner);
    }

    // assert reversed, where не содержит groups
    public static ClassExprWhere mapBack(Where where, final ImRevMap<BaseExpr, Expr> outerInner) {
        final Where outerWhere = Expr.andExprCheck(where, Expr.getWhere(outerInner.keys()));
        return new ExclPullWheres<ClassExprWhere, BaseExpr, Where>() {
            protected ClassExprWhere initEmpty() {
                return ClassExprWhere.FALSE;
            }
            protected ClassExprWhere proceedBase(Where data, ImMap<BaseExpr, BaseExpr> outerInner) {
                Result<ImRevMap<BaseExpr, BaseExpr>> innerOuter = new Result<>();
                Where where = Expr.andExprCheck(outerWhere, GroupExpr.getEqualsWhere(GroupExpr.groupMap(outerInner, data.getExprValues(), innerOuter)));
                return where.getClassWhere().mapBack(innerOuter.result).and(data.and(Expr.getWhere(outerInner)).getClassWhere()); // см. выше
            }
            protected ClassExprWhere add(ClassExprWhere op1, ClassExprWhere op2) {
                return op1.or(op2);
            }
        }.proceed(Where.TRUE(), outerInner);
    }

    private class OuterContext extends AbstractOuterContext<OuterContext> {

        protected OuterContext translate(MapTranslate translator) {
            return new ClassExprWhere(ClassExprWhere.this, translator.translateVariable(keySet().toRevMap())).getOuter();
        }

        public int hash(HashContext hash) {
            int result = 0;
            for(And<VariableSingleClassExpr> andWhere : wheres)
                result += AbstractOuterContext.hashKeysOuter(andWhere, hash);
            return result;
        }

        public ImSet<lsfusion.server.data.caches.OuterContext> calculateOuterDepends() {
            return BaseUtils.immutableCast(keySet());
        }

        protected boolean isComplex() {
            return true;
        }

        private ClassExprWhere getThis() {
            return ClassExprWhere.this;
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return getThis().equals(((OuterContext)o).getThis());
        }
    }
    private OuterContext outer;
    private OuterContext getOuter() {
        if(outer==null)
            outer = new OuterContext();
        return outer;
    }
    public ImSet<ParamExpr> getOuterKeys() {
        return getOuter().getOuterKeys();
    }
    public ImSet<Value> getOuterValues() {
        return getOuter().getOuterValues();
    }
    public int hashOuter(HashContext hashContext) {
        return getOuter().hashOuter(hashContext);
    }
    public ImSet<lsfusion.server.data.caches.OuterContext> getOuterDepends() {
        return getOuter().getOuterDepends();
    }
    public boolean enumerate(ContextEnumerator enumerator) {
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

    public ImSet<StaticValueExpr> getOuterStaticValues() {
        throw new RuntimeException("should not be");
    }
}
