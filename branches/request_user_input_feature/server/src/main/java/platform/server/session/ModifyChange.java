package platform.server.session;

import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractValuesContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.logics.property.PropertyInterface;

import java.util.Set;

public class ModifyChange<P extends PropertyInterface> extends AbstractValuesContext<ModifyChange<P>> {
    public final PropertyChange<P> change;
    public final boolean isFinal;

    public ModifyChange(PropertyChange<P> change, boolean isFinal) {
        this.change = change;
        this.isFinal = isFinal;
    }

    public int hash(HashValues hashValues) {
        return change.hashValues(hashValues) * 31 + (isFinal?1:0);
    }

    public QuickSet<Value> getValues() {
        return change.getInnerValues();
    }

    @Override
    public ModifyChange<P> translate(MapValuesTranslate mapValues) {
        return new ModifyChange<P>(change.translateValues(mapValues), isFinal);
    }

    @Override
    public boolean twins(TwinImmutableInterface o) {
        return change.equals(((ModifyChange)o).change) && isFinal == ((ModifyChange)o).isFinal;
    }

    public ModifyChange<P> add(ModifyChange<P> modify) {
        if(isFinal)
            return this;
        return new ModifyChange<P>(change.add(modify.change), modify.isFinal);
    }

    public static <P extends PropertyInterface> ModifyChange<P> addNull(ModifyChange<P> change1, ModifyChange<P> change2) {
        if(change1==null)
            return change2;
        if(change2==null)
            return change1;
        return change1.add(change2);
    }

    public boolean isEmpty() {
        return change.where.isFalse();
    }
}
