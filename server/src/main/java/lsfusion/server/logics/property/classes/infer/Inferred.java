package lsfusion.server.logics.property.classes.infer;

import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.lambda.set.NotFunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.Objects;

import static lsfusion.server.logics.property.classes.infer.NotNull.nullFilter;

public class Inferred<T extends PropertyInterface> {

    private final ImMap<T, ExClassSet> params;
    private final NotNull<T> notNull;
    private final ImSet<Compared<T>> compared;

    private final ImMap<T, ExClassSet> notParams;
    private final NotNull<T> notNotNull;
    private final ImSet<Compared<T>> notCompared;
    
    public static final Inferred FALSE = new Inferred(null, null, SetFact.EMPTY());
    public static <T extends PropertyInterface> Inferred<T> FALSE() {
        return FALSE;
    }

    private static <T extends PropertyInterface> ImMap<T, ExClassSet> applyCompared(ImMap<T, ExClassSet> params, ImSet<Compared<T>> compared, InferType inferType) {
        if (params == null) 
            return null;
        
        MAddExclMap<ImSet<Compared<T>>, ImMap<T, ExClassSet>> memoized = MapFact.mAddExclMap();
        return applyCompared(params, compared, inferType, memoized);
    }
    
    private static <T extends PropertyInterface> ImMap<T, ExClassSet> applyCompared(ImMap<T, ExClassSet> params, ImSet<Compared<T>> compared, InferType inferType, MAddExclMap<ImSet<Compared<T>>, ImMap<T, ExClassSet>> memoized) {
        if(params == null)
            return null;
        
        ImMap<T, ExClassSet> result = params;
        for(Compared<T> compare : compared) {
            ImSet<Compared<T>> recCompared = getNeededRecCompared(compare, compared, inferType);
            ImMap<T, ExClassSet> recInferred = memoized.get(recCompared);
            if(recInferred == null) {
                recInferred = applyCompared(result, recCompared, inferType);
                if (recInferred == null) {
                    return null;
                }
                memoized.exclAdd(recCompared, recInferred);
            }
            
            ExClassSet classSet = ExClassSet.op(compare.resolveInferred(compare.first, recInferred, inferType), compare.resolveInferred(compare.second, recInferred, inferType), false);
            ResolveClassSet vSet = null;
            if(compare instanceof Equals || ((vSet = ExClassSet.fromEx(classSet)) instanceof DataClass)) {
                Inferred<T> inferCompared = compare.inferResolved(compare.second, classSet, inferType).and(compare.inferResolved(compare.first, classSet, inferType), inferType);
                if (compare instanceof Relationed && !((DataClass) vSet).fixedSize())
                    inferCompared = inferCompared.orAny();
                ImMap<T, ExClassSet> inferredCompared = inferCompared.finishEx(inferType);
                if(inferredCompared == null)
                    return null;
                result = opParams(result, inferredCompared, false);
                if(result == null)
                    return null;
            }
        }
        return result;
    }

    // intersect optimization is really important when there are a lot of compares that can cause too deep recursion 
    private static <T extends PropertyInterface> ImSet<Compared<T>> getNeededRecCompared(Compared<T> compare, ImSet<Compared<T>> compared, InferType inferType) {
        ImMap<Compared<T>, ImSet<T>> comparedInterfaces = compared.mapValues((Compared<T> value) -> {
            ImSet<T> interfaces = SetFact.EMPTY();
            if (value.first.mapNeedInferredForValueClass(inferType))
                interfaces = interfaces.merge(value.first.getInterfaces().toSet());
            if (value.second.mapNeedInferredForValueClass(inferType))
                interfaces = interfaces.merge(value.second.getInterfaces().toSet());
            return interfaces;
        });
        
        // we need recursion to find all needed "linked" compared
        MOrderSet<Compared<T>> queue = SetFact.mOrderSet();
        queue.add(compare);
        int iq = 0;
        
        while(iq < queue.size()) {
            Compared<T> queued = queue.get(iq);
            ImSet<T> queuedInterfaces = comparedInterfaces.get(queued);
            for(Compared<T> depCompared : comparedInterfaces.filterFnValues(element -> element.intersect(queuedInterfaces)).keyIt())
                queue.add(depCompared);
            iq++;
        }
        return queue.immutableOrder().getSet().removeIncl(compare);
    }    

    private static <T> ImMap<T, ExClassSet> overrideClasses(ImMap<T, ExClassSet> oldClasses, ImMap<T, ExClassSet> newClasses) {
        return oldClasses.filterFnValues(new NotFunctionSet<>((SFunctionSet<ExClassSet>) element -> ExClassSet.fromEx(element) instanceof DataClass)).override(newClasses.removeNulls());
    }
    public ImMap<T, ExClassSet> finishEx(InferType inferType) {
        ImMap<T, ExClassSet> result = getParams(inferType);
        if(result == null)
            return null;
        if(notParams != null)
            result = overrideClasses(getNotParams(inferType), result);
        return result;
    }
    public ImMap<T, ExClassSet> getParams(InferType inferType) {
        return applyCompared(params, compared, inferType);
    }
    public ImSet<T>[] getNotNull() {
        return notNull == null ? null : notNull.getArray();
    }
    public ImMap<T, ExClassSet> getNotParams(InferType inferType) {
        return applyCompared(notParams, notCompared, inferType);
    }
    public ImSet<T>[] getNotNotNull() {
        return notNotNull == null ? null : notNotNull.getArray();
    }

    public Inferred(T paramDecl, ExClassSet set) {
        this(MapFact.singleton(paramDecl, set));
    }

    // предполагается что все не null пока
    public Inferred(ImMap<T, ExClassSet> params) {
        this(checkNull(params), SetFact.EMPTY());
    }

    // внутренний конструктор для верхнего конструктора
    private Inferred(ImMap<T, ExClassSet> params, ImSet<Compared<T>> compared) {
        this(params, params == null ? null : new NotNull<>(params.keys()), compared);
    }

    public Inferred(ImMap<T, ExClassSet> params, NotNull<T> notNull, ImSet<Compared<T>> compared) {
        this(params, notNull, compared, MapFact.EMPTY(), NotNull.EMPTY(), SetFact.EMPTY());
    }

    private static <T extends PropertyInterface> Inferred<T> checkNull(ImMap<T, ExClassSet> params, NotNull<T> notNull, ImSet<Compared<T>> compared, ImMap<T, ExClassSet> notParams, NotNull<T> notNotNull, ImSet<Compared<T>> notCompared) {
        if(params == null) {
            notNull = null;
            compared = SetFact.EMPTY();
        }
        if(notParams == null) {
            notNotNull = null;
            notCompared = SetFact.EMPTY();
        }
        return new Inferred<>(params, notNull, compared, notParams, notNotNull, notCompared);
    }

    public Inferred(ImMap<T, ExClassSet> params, NotNull<T> notNull, ImSet<Compared<T>> compared, ImMap<T, ExClassSet> notParams, NotNull<T> notNotNull, ImSet<Compared<T>> notCompared) {
        this.params = params;
        this.notNull = notNull;
        this.compared = compared;
        this.notParams = notParams;
        this.notNotNull = notNotNull;
        this.notCompared = notCompared;
    }

    private Inferred(Inferred<T> inferred, boolean not) {
        this(inferred.notParams, inferred.notNotNull, inferred.notCompared, inferred.params, inferred.notNull, inferred.compared);
        assert not;
    }
    
    public Inferred<T> not() {
        return new Inferred<>(this, true);
    }

    private Inferred() {
        this(MapFact.EMPTY());
    }

    private Inferred(Compared<T> compared) {
        this(MapFact.EMPTY(), NotNull.EMPTY(), SetFact.singleton(compared));
    }
    
    public static <T extends PropertyInterface> Inferred<T> create(Compared<T> compare, InferType inferType, boolean not) {
        Inferred<T> result = new Inferred<>(compare);
        if(not)
            result = result.not();
        return result.and(compare.inferResolved(compare.first, null, inferType), inferType).and(compare.inferResolved(compare.second, null, inferType), inferType);
    }

    public static final Inferred EMPTY = new Inferred();
    public static <T extends PropertyInterface> Inferred<T> EMPTY() {
        return EMPTY;
    }

    private static <T> ImMap<T, ExClassSet> opParams(final ImMap<T, ExClassSet> op1, final ImMap<T, ExClassSet> op2, final boolean or) {
        if((op1 == null || op2 == null) && !or)
            return null;
        if(op1 == null)
            return op2;
        if(op2 == null)
            return op1;

        ImMap<T, ExClassSet> result = ExClassSet.op(op1.keys().merge(op2.keys()), op1, op2, or);
        if(!or)
            return checkNull(result);
        return result;
    }

    public static <T> ImMap<T, ExClassSet> checkNull(ImMap<T, ExClassSet> result) {
        for(ExClassSet exClass : result.valueIt())
            if(exClass != null && exClass.isEmpty())
                return null;
        return result;
    }

    private static <T extends PropertyInterface> Pair<ImMap<T, ExClassSet>, ImSet<Compared<T>>> or(ImMap<T, ExClassSet> params1, ImSet<Compared<T>> compared1, ImMap<T, ExClassSet> params2, ImSet<Compared<T>> compared2, InferType inferType) {
        Result<ImSet<Compared<T>>> rest1 = new Result<>();
        Result<ImSet<Compared<T>>> rest2 = new Result<>();
        ImSet<Compared<T>> common = compared1.split(compared2, rest1, rest2);
        return new Pair<>(opParams(applyCompared(params1, rest1.result, inferType), applyCompared(params2, rest2.result, inferType), true), common);
    }

    private static <T extends PropertyInterface> NotNull<T> opNotNull(NotNull<T> notNull1, NotNull<T> notNull2, boolean or) {
        if((notNull1 == null || notNull2 == null) && !or)
            return null;
        if(notNull1 == null)
            return notNull2;
        if(notNull2 == null)
            return notNull1;

        if(or)
            return notNull1.or(notNull2);
        else
            return notNull1.and(notNull2);
    }
    
    public Inferred<T> or(Inferred<T> or2, InferType inferType) {
        Pair<ImMap<T, ExClassSet>, ImSet<Compared<T>>> or = or(params, compared, or2.params, or2.compared, inferType);
        return checkNull(or.first, opNotNull(notNull, or2.notNull, true), or.second, opParams(notParams, or2.notParams, false), opNotNull(notNotNull, or2.notNotNull, false), notCompared.merge(or2.notCompared));
    }

    public Inferred<T> and(Inferred<T> or2, InferType inferType) {
        Pair<ImMap<T, ExClassSet>, ImSet<Compared<T>>> orNot = or(notParams, notCompared, or2.notParams, or2.notCompared, inferType);
        return checkNull(opParams(params, or2.params, false), opNotNull(notNull, or2.notNull, false), compared.merge(or2.compared), orNot.first, opNotNull(notNotNull, or2.notNotNull, true) ,orNot.second);
    }
    
    public Inferred<T> op(Inferred<T> op, boolean or, InferType inferType) {
        if(or)
            return or(op, inferType);
        else
            return and(op, inferType);
    }

    public <P extends PropertyInterface> Inferred<P> map(ImRevMap<T, P> mapping) {
        return new Inferred<>(MapFact.nullCrossJoin(params, mapping), NotNull.nullMapRev(notNull, mapping), Compared.map(compared, mapping), MapFact.nullCrossJoin(notParams, mapping), NotNull.nullMapRev(notNotNull, mapping), Compared.map(notCompared, mapping));
    }
    
    private static <T extends PropertyInterface> Pair<ImMap<T, ExClassSet>, ImSet<Compared<T>>> applyCompared(ImSet<T> set, ImSet<Compared<T>> compared, ImMap<T, ExClassSet> params, InferType inferType) {
        ImSet<Compared<T>> mixed = Compared.mixed(compared, set);
        return new Pair<>(applyCompared(params, mixed, inferType), compared.removeIncl(mixed));
    }
    
    public Inferred<T> applyCompared(ImSet<T> set, InferType inferType) {
        Pair<ImMap<T, ExClassSet>, ImSet<Compared<T>>> applied = applyCompared(set, compared, params, inferType); 
        Pair<ImMap<T, ExClassSet>, ImSet<Compared<T>>> appliedNot = applyCompared(set, notCompared, notParams, inferType);
        return checkNull(applied.first, notNull, applied.second, appliedNot.first, notNotNull, appliedNot.second);
    }

    public Inferred<T> remove(ImSet<T> remove) {
        return new Inferred<>(MapFact.nullRemove(params, remove), NotNull.nullRemove(notNull, remove), Compared.remove(compared, remove), MapFact.nullRemove(notParams, remove), NotNull.nullRemove(notNotNull, remove), Compared.remove(notCompared, remove));
    }
    public Inferred<T> keep(ImSet<T> keep) {
        return new Inferred<>(MapFact.nullFilter(params, keep), nullFilter(notNull, keep), Compared.keep(compared, keep), MapFact.nullFilter(notParams, keep), nullFilter(notNotNull, keep), Compared.keep(notCompared, keep));
    }

    public Inferred<T> orAny() { // или null или any
        return orAny(SetFact.EMPTY());
    }
    public Inferred<T> orAny(ImSet<T> notNullParam) { // или null или any
        return new Inferred<>(params == null ? MapFact.EMPTY() : params.mapValues((key, value) -> notNullParam.contains(key) ? value : ExClassSet.orAny(value)), new NotNull<>(notNullParam), SetFact.EMPTY());
    }

    public boolean isEmpty(InferType inferType) {
        return finishEx(inferType) == null;
//        ImMap<T, ExClassSet> inferred = finishEx(inferType);
//        for(ExClassSet exClass : inferred.valueIt())
//            if(exClass != null && exClass.isEmpty())
//                return true;
//        return false;
    }

    public boolean isNotNull(ImSet<T> interfaces, InferType inferType) {
        if(isEmpty(inferType))
            return true;
        return notNull.isNotNull(interfaces);
    }

    public boolean isFull(Iterable<T> checkInterfaces, InferType inferType) {
        ImMap<T, ExClassSet> inferred = finishEx(inferType);
        if(inferred == null)
            return true;
        for (T checkInterface : checkInterfaces) {
            ExClassSet exClassSet = inferred.get(checkInterface);
            if (exClassSet == null || exClassSet.orAny)
                return false;
        }
        return true;
    }

    private static <T extends PropertyInterface> ImMap<T, ExClassSet> getBase(ImMap<T, ExClassSet> map) {
        return map == null ? null : map.mapValues(ExClassSet::getBase);
    }
    public Inferred<T> getBase(InferType inferType) {
        return new Inferred<>(getBase(getParams(inferType)), notNull, SetFact.EMPTY(), getBase(getNotParams(inferType)), notNotNull, SetFact.EMPTY());
    }

    // only for check caches
    public boolean equals(Object o) {
        return this == o || o instanceof Inferred && Objects.equals(params, ((Inferred<?>) o).params) && Objects.equals(notNull, ((Inferred<?>) o).notNull) && Objects.equals(compared, ((Inferred<?>) o).compared) && Objects.equals(notParams, ((Inferred<?>) o).notParams) && Objects.equals(notNotNull, ((Inferred<?>) o).notNotNull) && Objects.equals(notCompared, ((Inferred<?>) o).notCompared);
    }

    public int hashCode() {
        return Objects.hash(params, notNull, compared, notParams, notNotNull, notCompared);
    }
}
