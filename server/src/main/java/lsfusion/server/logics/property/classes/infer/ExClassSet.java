package lsfusion.server.logics.property.classes.infer;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.classes.user.set.ResolveUpClassSet;
import lsfusion.server.physics.dev.id.resolve.SignatureMatcher;

import java.util.List;
import java.util.function.Function;

public class ExClassSet extends TwinImmutableObject {
    private final ResolveClassSet classSet;

    public final ImSet<Object> values;    
    public final boolean orAny;

    public static final ExClassSet FALSE = new ExClassSet(null);

    public static <T> boolean containsAll(ImSet<T> keys, ImMap<T, ExClassSet> classes1, ImMap<T, ExClassSet> classes2, boolean ignoreAbstracts) {
        ImMap<T, ResolveClassSet> exClasses1 = ExClassSet.fromEx(classes1);
        ImMap<T, ResolveClassSet> exClasses2 = ExClassSet.fromEx(classes2);

        if(ignoreAbstracts) {
            return new ClassWhere<>(ResolveUpClassSet.toAnd(exClasses2)).meansCompatible(new ClassWhere<>(ResolveUpClassSet.toAnd(exClasses1)));
        } else {
            ImOrderSet<T> orderKeys = keys.toOrderSet();
            List<ResolveClassSet> listClasses = orderKeys.mapListValues(exClasses1.fnGetValue()).toJavaList();
            List<ResolveClassSet> listPropClasses = orderKeys.mapListValues(exClasses2.fnGetValue()).toJavaList();
            return SignatureMatcher.isCompatible(listClasses, listPropClasses, true, true);
        }
    }

    public static <T> boolean intersect(ImSet<T> keys, ImMap<T, ExClassSet> classes1, ImMap<T, ExClassSet> classes2) {
        return Inferred.checkNull(op(keys, classes1, classes2, false)) != null;
    }

    public static <T> ImMap<T, ExClassSet> op(ImSet<T> keys, final ImMap<T, ExClassSet> op1, final ImMap<T, ExClassSet> op2, final boolean or) {
        return keys.mapValues((T value) -> op(op1.get(value), op2.get(value), or));
    }

    @Override
    protected boolean calcTwins(TwinImmutableObject o) {
        return orAny == ((ExClassSet) o).orAny && BaseUtils.nullEquals(classSet, ((ExClassSet) o).classSet) && BaseUtils.nullEquals(values , ((ExClassSet) o).values);
    }

    @Override
    public int immutableHashCode() {
        return 31 * (31 * BaseUtils.nullHash(classSet) + BaseUtils.nullHash(values)) + ( orAny ? 1 : 0 );
    }

    public boolean isEmpty() {
        return !orAny && (classSet == null || classSet.isEmpty() || (values!=null && values.isEmpty()));
    }
    
    @Override
    public String toString() {
        return (classSet == null ? "FALSE" : classSet) + (orAny ? "(or any)" : "");
    } 
    
    // маркер, если решим перейти на другую схему (когда нужно при NULL значении в Join или Order идти (см. orAny) идти на or, а не and)
    public static final ExClassSet NULL = null;   
    public static ExClassSet notNull(ExClassSet set) {
        return null;
    }
    public static ExClassSet toNotNull(ExClassSet set) { // преобразование, там где сейчас по умолчанию notNull - Group By, Compare
        return set;
    }

    public ExClassSet(ResolveClassSet classSet) {
        this(classSet, false);
    }

    public ExClassSet(ResolveClassSet classSet, Object value) {
        this(classSet, SetFact.singleton(value), false);
    }

    public ExClassSet(ResolveClassSet classSet, boolean orAny) {
        this(classSet, null, orAny);
    }

    public static ExClassSet removeValues(ExClassSet set) {
        if(set == null || set.values == null)
            return set;
        return new ExClassSet(set.classSet, set.orAny);
    }

    public static ExClassSet toEx(ResolveClassSet set) {
        if(set == null) return null;
        return new ExClassSet(set);
    }

    public static ExClassSet toExAny(ResolveClassSet set) {
        if(set == null) return null;
        return new ExClassSet(set, true);
    }

    public static ExClassSet toExValue(ValueClass set) {
        if(set == null) return null;
        return new ExClassSet(set.getResolveSet());
    }

    public static ExClassSet toExType(Type set) {
        if(set == null) return null;
        return new ExClassSet((DataClass)set);
    }

    public static ResolveClassSet fromEx(ExClassSet set) {
        if(set == null) return null;
        assert set.orAny || set.classSet != null;
        return set.classSet;
    }

    public static ResolveClassSet fromExAnd(ExClassSet set) {
        return fromEx(set);
    }

    public static Type fromExType(ExClassSet set) {
        ResolveClassSet andSet = fromExAnd(set);
        if(andSet == null) return null;
        return andSet.getType();
    }

    public static ValueClass fromExValue(ExClassSet set) {
        ResolveClassSet orSet = fromEx(set);
        if(orSet == null) return null;
        return orSet.getCommonClass();
    }

    public static ValueClass fromResolveValue(ResolveClassSet set) {
        if(set == null) return null;
        return set.getCommonClass();
    }

    public final static ExClassSet logical = new ExClassSet(LogicalClass.instance);

    public ExClassSet(ResolveClassSet classSet, ImSet<Object> values, boolean orAny) {
        this.classSet = classSet;
        this.values = values;
        this.orAny = orAny;
    }

    public static <T> ImMap<T, ExClassSet> toEx(ImMap<T, ResolveClassSet> classes) {
        return classes.mapValues(value -> toEx(value));
    }

    public static <T> ImMap<T, ExClassSet> toExAny(ImMap<T, ResolveClassSet> classes) {
        return classes.mapValues(value -> toExAny(value));
    }

    public static <T> ImMap<T, ExClassSet> toExValue(ImMap<T, ValueClass> classes) {
        return classes.mapValues(value -> toExValue(value));
    }

    public static <T> ImMap<T, ResolveClassSet> fromEx(ImMap<T, ExClassSet> classes) {
        return classes.mapValues(value -> fromEx(value));
    }

    public static <T> ImMap<T, ResolveClassSet> fromExAnd(ImMap<T, ExClassSet> classes) {
        return classes.mapValues(set -> fromExAnd(set));
    }

    public static <T> ImMap<T, ValueClass> fromExValue(ImMap<T, ExClassSet> classes) {
        return classes.mapValues(set -> fromExValue(set));
    }

    public static <T> ImMap<T, ValueClass> fromResolveValue(ImMap<T, ResolveClassSet> classes) {
        return classes.mapValues(set -> fromResolveValue(set));
    }

//    public static List<ExClassSet> toEx(List<ResolveClassSet> classes) {
//        if(classes == null)
//            return null;
//
//        List<ExClassSet> result = new ArrayList<ExClassSet>();
//        for(ResolveClassSet paramClass : classes) {
//            result.add(toEx(paramClass));
//        }
//        return result;
//    }
//
//    public static List<ResolveClassSet> fromEx(List<ExClassSet> classes) {
//        if(classes == null)
//            return null;
//
//        List<ResolveClassSet> result = new ArrayList<ResolveClassSet>();
//        for(ExClassSet paramClass : classes) {
//            result.add(fromEx(paramClass));
//        }
//        return result;
//    }

    public ExClassSet opNull(boolean or) {
        if(or)
            return orAny();
        return this;
    }

    // на конфликты типов (STRING - DATE например) по сути проверяет
    public static ExClassSet checkNull(ResolveClassSet set, ImSet<Object> values, boolean orAny) {
        return new ExClassSet(set, values, orAny);
    }
    
    private static ImSet<Object> op(ImSet<Object> values1, ImSet<Object> values2, boolean or) {
        if(or) {
            if (values1 == null || values2 == null)
                return null;
            return values1.merge(values2);
        } else {
            if (values1 == null)
                return values2;
            if(values2 == null)
                return values1;
            return values1.filter(values2);
        }
    } 
    
    private static ResolveClassSet op(ResolveClassSet set1, ResolveClassSet set2, boolean or) {
        if(set1 == null) {
            if(or)
                return set2;
            else
                return null;
        }
        if(set2 == null) {
            if(or)
                return set1;
            else
                return null;
        }
        return or ? set1.or(set2) : set1.and(set2);
    }
    public static ExClassSet op(ResolveClassSet set1, ResolveClassSet set2, ImSet<Object> values1, ImSet<Object> values2, boolean or, boolean orAny) {
        return checkNull(op(set1, set2, or), op(values1, values2, or) , orAny);
    }

    // может быть null
    public ExClassSet op(ExClassSet exClassSet, boolean or) {
        if(or) {
            return op(classSet, exClassSet.classSet, values, exClassSet.values, true, orAny || exClassSet.orAny);
        } else {
            if(orAny) {
                if(exClassSet.orAny) // если оба orAny, то не and'м, а or'м
                    return op(classSet, exClassSet.classSet, values, exClassSet.values, true, true);
                return exClassSet;
            }
            if(exClassSet.orAny)
                return this;
            return op(classSet, exClassSet.classSet, values, exClassSet.values, false, false);
        }
    }

    public static ExClassSet op(ExClassSet class1, ExClassSet class2, boolean or) {
        if(class1 == null && class2 == null)
            return null;

        if(class1 == null)
            return class2.opNull(or);

        if(class2 == null)
            return class1.opNull(or);

        return class1.op(class2, or);
    }

    public static ExClassSet orAny(ExClassSet set) {
        if(set == null) return set;
        return set.orAny();
    }
    public ExClassSet orAny() {
        if(orAny)
            return this;
        return new ExClassSet(classSet, values, true);
    }

    public static ExClassSet getBase(ExClassSet set) {
        if(set == null) return null;
        return new ExClassSet(set.classSet == null ? null : set.classSet.getCommonClass().getBaseClass().getResolveSet(), null, set.orAny);
    }
}
