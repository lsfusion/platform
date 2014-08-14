package lsfusion.server.logics.property.infer;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.type.Type;

public class ExClassSet extends TwinImmutableObject {
    private final ResolveClassSet classSet;

    public final ImSet<Object> values;    
    public final boolean orAny;

    public static final ExClassSet FALSE = new ExClassSet(null); 

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
    public static final ExClassSet notNull(ExClassSet set) {
        return null;
    }
    public static final ExClassSet toNotNull(ExClassSet set) { // преобразование, там где сейчас по умолчанию notNull - Group By, Compare
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

    public static final ExClassSet removeValues(ExClassSet set) {
        if(set == null || set.values == null)
            return set;
        return new ExClassSet(set.classSet, set.orAny);
    }

    public static ExClassSet toEx(ResolveClassSet set) {
        if(set == null) return null;
        return new ExClassSet(set);
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
    
    public final static ExClassSet logical = new ExClassSet(LogicalClass.instance);

    public ExClassSet(ResolveClassSet classSet, ImSet<Object> values, boolean orAny) {
        this.classSet = classSet;
        this.values = values;
        this.orAny = orAny;
    }

    public static <T> ImMap<T, ExClassSet> toEx(ImMap<T, ResolveClassSet> classes) {
        return classes.mapValues(new GetValue<ExClassSet, ResolveClassSet>() {
            public ExClassSet getMapValue(ResolveClassSet value) {
                return toEx(value);
            }});
    }

    public static <T> ImMap<T, ExClassSet> toExValue(ImMap<T, ValueClass> classes) {
        return classes.mapValues(new GetValue<ExClassSet, ValueClass>() {
            public ExClassSet getMapValue(ValueClass value) {
                return toExValue(value);
            }});
    }

    public static <T> ImMap<T, ResolveClassSet> fromEx(ImMap<T, ExClassSet> classes) {
        return classes.mapValues(new GetValue<ResolveClassSet, ExClassSet>() {
            public ResolveClassSet getMapValue(ExClassSet value) {
                return fromEx(value);
            }
        });
    }

    public static <T> ImMap<T, ResolveClassSet> fromExAnd(ImMap<T, ExClassSet> classes) {
        return classes.mapValues(new GetValue<ResolveClassSet, ExClassSet>() {
            public ResolveClassSet getMapValue(ExClassSet value) {
                return fromExAnd(value);
            }
        });
    }

    public static <T> ImMap<T, ValueClass> fromExValue(ImMap<T, ExClassSet> classes) {
        return classes.mapValues(new GetValue<ValueClass, ExClassSet>() {
            public ValueClass getMapValue(ExClassSet value) {
                return fromExValue(value);
            }
        });
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
        if(values1 == null || values2 == null)
            return null;
        return or ? values1.merge(values2) : values1.filter(values2);
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
