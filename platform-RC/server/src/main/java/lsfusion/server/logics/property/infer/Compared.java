package lsfusion.server.logics.property.infer;

import lsfusion.base.SFunctionSet;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;

public abstract class Compared<T extends PropertyInterface> {

    public final CalcPropertyInterfaceImplement<T> first;
    public final CalcPropertyInterfaceImplement<T> second;

    protected Compared(CalcPropertyInterfaceImplement<T> first, CalcPropertyInterfaceImplement<T> second) {
        this.first = first;
        this.second = second;
    }

    public abstract ExClassSet resolveInferred(CalcPropertyInterfaceImplement<T> operand, ImMap<T, ExClassSet> inferred, InferType inferType);
    public abstract Inferred<T> inferResolved(CalcPropertyInterfaceImplement<T> operand, ExClassSet classSet, InferType inferType);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Compared compared = (Compared) o;

        return (first.equals(compared.first) && second.equals(compared.second)) || (first.equals(compared.second) && second.equals(compared.first));
    }

    @Override
    public int hashCode() {
        return first.hashCode() + second.hashCode();
    }
    
    protected abstract <P extends PropertyInterface> Compared<P> create(CalcPropertyInterfaceImplement<P> first, CalcPropertyInterfaceImplement<P> second);
    
    public <P extends PropertyInterface> Compared<P> remap(ImRevMap<T, P> mapping) {
        return create(first.map(mapping), second.map(mapping));
    }
    
    public static <T extends PropertyInterface, P extends PropertyInterface> ImSet<Compared<P>> map(ImSet<Compared<T>> compareds, final ImRevMap<T, P> mapping) {
        return compareds.mapSetValues(new GetValue<Compared<P>, Compared<T>>() {
            public Compared<P> getMapValue(Compared<T> value) {
                return value.remap(mapping);
            }
        });
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
