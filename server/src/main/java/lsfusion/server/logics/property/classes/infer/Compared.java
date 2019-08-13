package lsfusion.server.logics.property.classes.infer;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class Compared<T extends PropertyInterface> {

    public final PropertyInterfaceImplement<T> first;
    public final PropertyInterfaceImplement<T> second;

    protected Compared(PropertyInterfaceImplement<T> first, PropertyInterfaceImplement<T> second) {
        this.first = first;
        this.second = second;
    }

    public abstract ExClassSet resolveInferred(PropertyInterfaceImplement<T> operand, ImMap<T, ExClassSet> inferred, InferType inferType);
    public abstract Inferred<T> inferResolved(PropertyInterfaceImplement<T> operand, ExClassSet classSet, InferType inferType);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Compared<T> compared = (Compared<T>) o;

        return (first.mapEquals(compared.first) && second.mapEquals(compared.second)) || (first.mapEquals(compared.second) && second.mapEquals(compared.first));
    }

    @Override
    public int hashCode() {
        return first.mapHashCode() + second.mapHashCode();
    }
    
    protected abstract <P extends PropertyInterface> Compared<P> create(PropertyInterfaceImplement<P> first, PropertyInterfaceImplement<P> second);
    
    public <P extends PropertyInterface> Compared<P> remap(ImRevMap<T, P> mapping) {
        return create(first.map(mapping), second.map(mapping));
    }
    
    public static <T extends PropertyInterface, P extends PropertyInterface> ImSet<Compared<P>> map(ImSet<Compared<T>> compareds, final ImRevMap<T, P> mapping) {
        return compareds.mapSetValues(value -> value.remap(mapping));
    }
    
    public boolean intersect(ImSet<T> interfaces) {
        return first.getInterfaces().toSet().intersect(interfaces) || second.getInterfaces().toSet().intersect(interfaces);
    }

    public boolean keep(ImSet<T> interfaces) {
        return interfaces.containsAll(first.getInterfaces().toSet()) && interfaces.containsAll(second.getInterfaces().toSet());
    }

    public static <T extends PropertyInterface> ImSet<Compared<T>> remove(ImSet<Compared<T>> compareds, final ImSet<T> remove) {
        return compareds.filterFn(new SFunctionSet<Compared<T>>() {
            public boolean contains(Compared<T> element) {
                return !element.intersect(remove);
            }
        });
    }

    public static <T extends PropertyInterface> ImSet<Compared<T>> keep(ImSet<Compared<T>> compareds, final ImSet<T> keep) {
        return compareds.filterFn(new SFunctionSet<Compared<T>>() {
            public boolean contains(Compared<T> element) {
                return element.keep(keep);
            }
        });
    }

    public static <T extends PropertyInterface> ImSet<Compared<T>> mixed(ImSet<Compared<T>> compareds, final ImSet<T> mixed) {
        return compareds.filterFn(new SFunctionSet<Compared<T>>() {
            public boolean contains(Compared<T> element) {
                return element.intersect(mixed) && !element.keep(mixed);
            }
        });
    }
}
