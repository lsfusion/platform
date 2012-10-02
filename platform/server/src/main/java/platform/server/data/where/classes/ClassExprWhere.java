package platform.server.data.where.classes;

import platform.base.*;
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

import java.util.Collection;
import java.util.Map;

public class ClassExprWhere extends AbstractClassWhere<VariableClassExpr, ClassExprWhere> implements DNFWheres.Interface<ClassExprWhere>, OuterContext<ClassExprWhere>, KeyType {

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
        for(And<VariableClassExpr> where : wheres) {
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

    public boolean means(CheckWhere where) {
        return getPackWhere().means(where);
    }

    private ClassExprWhere(boolean isTrue) {
        super(isTrue);
    }

    public ClassExprWhere(And<VariableClassExpr> where) {
        super(where);
    }

    public static ClassExprWhere TRUE = new ClassExprWhere(true);
    public static ClassExprWhere FALSE = new ClassExprWhere(false);

    protected ClassExprWhere FALSE() {
        return FALSE;
    }

    public ClassExprWhere(VariableClassExpr key, AndClassSet classes) {
        super(key,classes);
    }

    public ClassExprWhere(QuickMap<KeyExpr,AndClassSet> andKeys) {
        this(new And<VariableClassExpr>(andKeys));
    }

    public ClassExprWhere(Map<? extends VariableClassExpr,ValueClass> andKeys) {
        super((Map<VariableClassExpr,ValueClass>) andKeys, true);
    }

    private ClassExprWhere(And<VariableClassExpr>[] iWheres) {
        super(iWheres);
    }
    protected ClassExprWhere createThis(And<VariableClassExpr>[] wheres) {
        return new ClassExprWhere(wheres);
    }

    private static And<VariableClassExpr> andEquals(And<VariableClassExpr> and, EqualMap equals) {
        And<VariableClassExpr> result = new And<VariableClassExpr>(and);
        for(int i=0;i<equals.num;i++) {
            Equal equal = equals.comps[i];
            if(!equal.dropped) {
                AndClassSet andClasses = null;
                for(int j=0;j<equal.size;j++)
                    if(equal.exprs[j] instanceof VariableClassExpr) {
                        AndClassSet classes = and.getPartial((VariableClassExpr) equal.exprs[j]);
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
                        if(equal.exprs[j] instanceof VariableClassExpr)
                            result.set((VariableClassExpr)equal.exprs[j], andClasses);
            }
        }
        return result;
    }
    // нужен очень быстрый так как в checkTrue используется
    public ClassExprWhere andEquals(EqualMap equals) {
        if(equals.size==0 || isFalse() || isTrue()) return this;

        And<VariableClassExpr>[] rawAndWheres = newArray(wheres.length); int num=0;
        for(And<VariableClassExpr> where : wheres) {
            And<VariableClassExpr> andWhere = andEquals(where,equals);
            if(andWhere!=null)
                rawAndWheres[num++] = andWhere;
        }
        And<VariableClassExpr>[] andWheres = newArray(num); System.arraycopy(rawAndWheres,0,andWheres,0,num);
        return new ClassExprWhere(andWheres);
    }

    private ClassExprWhere(ClassExprWhere classes, Map<VariableClassExpr, VariableClassExpr> map) {
        super(classes, map);
    }

    // получает классы для BaseExpr'ов
    public <K> ClassWhere<K> get(Map<K, ? extends BaseExpr> map) {
        ClassWhere<K> transWhere = ClassWhere.STATIC(false);
        for(And<VariableClassExpr> andWhere : wheres) {
            boolean isFalse = false;
            And<K> andTrans = new And<K>();
            for(Map.Entry<K, ? extends BaseExpr> mapEntry : map.entrySet()) {
                AndClassSet classSet = mapEntry.getValue().getAndClassSet(andWhere);
                if(classSet!=null && !andTrans.add(mapEntry.getKey(), classSet)) {
                    isFalse = true;
                    break;
                }
            }
            if(!isFalse)
                transWhere = transWhere.or(new ClassWhere<K>(andTrans));
        }
        return transWhere;
    }
    // тоже самое что сверху но с assertion'ом что reversed
    public <K> ClassWhere<K> map(Map<? extends VariableClassExpr, K> map) {
        ClassWhere<K> transWhere = ClassWhere.STATIC(false);
        for(And<VariableClassExpr> andWhere : wheres) {
            And<K> andTrans = new And<K>();
            for(Map.Entry<? extends VariableClassExpr, K> mapEntry : map.entrySet()) {
                boolean added = andTrans.add(mapEntry.getValue(), andWhere.get(mapEntry.getKey()));
                assert added;
            }
            transWhere = transWhere.or(new ClassWhere<K>(andTrans));
        }
        return transWhere;
    }

    public AndClassSet getAndClassSet(BaseExpr expr) {
        AndClassSet result = null;
        for(And<VariableClassExpr> andWhere : wheres) {
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

    public NotNullExprSet getExprFollows() {
        NotNullExprSet[] follows = new NotNullExprSet[wheres.length] ; int num = 0;
        for(And<VariableClassExpr> where : wheres) {
            NotNullExprSet result = new NotNullExprSet();
            for(int i=0;i<where.size;i++)
                result.addAll(where.getKey(i).getExprFollows(true, true));
            follows[num++] = result;
        }
        return new NotNullExprSet(follows);
    }

    // здесь не обязательно есть все BaseExpr'ы, равно как и то что они не повторяются
    public ClassExprWhere mapBack(Map<BaseExpr, ? extends BaseExpr> map) {
        ClassExprWhere transWhere = ClassExprWhere.FALSE;
        for(And<VariableClassExpr> andWhere : wheres) {
            boolean isFalse = false;
            And<VariableClassExpr> andTrans = new And<VariableClassExpr>();
            for(Map.Entry<BaseExpr, ? extends BaseExpr> mapEntry : map.entrySet()) {
                AndClassSet mapSet = mapEntry.getValue().getAndClassSet(andWhere);
                if(mapSet==null)
                    continue;
                if(!mapEntry.getKey().addAndClassSet(andTrans,mapSet)) {
                    isFalse = true;
                    break;
                }
            }
            if(!isFalse)
                transWhere = transWhere.or(new ClassExprWhere(andTrans));
        }
        return transWhere;
    }

    // assert reversed, where содержит groups
    public static ClassExprWhere mapBack(Map<BaseExpr, ? extends Expr> outerInner, Where innerWhere) {
        return new ExclPullWheres<ClassExprWhere, BaseExpr, Where>() {
            protected ClassExprWhere initEmpty() {
                return ClassExprWhere.FALSE;
            }
            protected ClassExprWhere proceedBase(Where data, Map<BaseExpr, BaseExpr> outerInner) {
                return data.getClassWhere().mapBack(outerInner);
            }
            protected ClassExprWhere add(ClassExprWhere op1, ClassExprWhere op2) {
                return op1.or(op2);
            }
        }.proceed(innerWhere, outerInner);
    }

    // assert reversed, where не содержит groups
    public static ClassExprWhere mapBack(Where where, Map<Expr, BaseExpr> innerOuter) {
        final Where outerWhere = where.and(Expr.getWhere(innerOuter.values()));
        return new ExclPullWheres<ClassExprWhere, BaseExpr, Where>() {
            protected ClassExprWhere initEmpty() {
                return ClassExprWhere.FALSE;
            }
            protected ClassExprWhere proceedBase(Where data, Map<BaseExpr, BaseExpr> outerInner) {
                ReversedMap<BaseExpr, BaseExpr> innerOuter = new ReversedHashMap<BaseExpr, BaseExpr>();
                Where where = outerWhere.and(GroupExpr.getEqualsWhere(GroupExpr.groupMap(outerInner, outerWhere.getExprValues(), innerOuter)));
                return where.getClassWhere().mapBack(innerOuter).and(data.getClassWhere());
            }
            protected ClassExprWhere add(ClassExprWhere op1, ClassExprWhere op2) {
                return op1.or(op2);
            }
        }.proceed(Expr.getWhere(innerOuter.keySet()), BaseUtils.reverse(innerOuter));
    }

    private class OuterContext extends AbstractOuterContext<OuterContext> {

        protected OuterContext translate(MapTranslate translator) {
            return new ClassExprWhere(ClassExprWhere.this, translator.translateVariable(BaseUtils.toMap(keySet()))).getOuter();
        }

        protected int hash(HashContext hash) {
            int result = 0;
            for(And<VariableClassExpr> andWhere : wheres)
                result += AbstractOuterContext.hashKeysOuter(andWhere, hash);
            return result;
        }

        public QuickSet<platform.server.caches.OuterContext> calculateOuterDepends() {
            return new QuickSet<platform.server.caches.OuterContext>(keySet());
        }

        protected boolean isComplex() {
            return true;
        }

        private ClassExprWhere getThis() {
            return ClassExprWhere.this;
        }

        public boolean twins(TwinImmutableInterface o) {
            return getThis().equals(((OuterContext)o).getThis());
        }
    }
    private OuterContext outer;
    private OuterContext getOuter() {
        if(outer==null)
            outer = new OuterContext();
        return outer;
    }
    public QuickSet<KeyExpr> getOuterKeys() {
        return getOuter().getOuterKeys();
    }
    public QuickSet<Value> getOuterValues() {
        return getOuter().getOuterValues();
    }
    public int hashOuter(HashContext hashContext) {
        return getOuter().hashOuter(hashContext);
    }
    public QuickSet<platform.server.caches.OuterContext> getOuterDepends() {
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
    public ClassExprWhere remove(Collection<? extends VariableClassExpr> keys) {
        ClassExprWhere result = ClassExprWhere.FALSE;
        for(And<VariableClassExpr> andWhere : wheres)
            result = result.or(new ClassExprWhere(andWhere.remove(keys)));
        return result;
    }

    public ClassExprWhere keep(Collection<? extends VariableClassExpr> keys) {
        ClassExprWhere result = ClassExprWhere.FALSE;
        for(And<VariableClassExpr> andWhere : wheres)
            result = result.or(new ClassExprWhere((And<VariableClassExpr>) andWhere.keep(keys)));
        return result;
    }

}
