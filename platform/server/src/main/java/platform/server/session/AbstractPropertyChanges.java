package platform.server.session;

import platform.base.BaseUtils;
import platform.base.QuickMap;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.ManualLazy;
import platform.server.caches.MapValues;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.util.HashSet;
import java.util.Set;

// Immutable
public abstract class AbstractPropertyChanges<P extends PropertyInterface, T extends Property<P>, This extends AbstractPropertyChanges<P,T,This>> extends QuickMap<T, PropertyChange<P>> implements MapValues<This> {

    protected abstract This createThis();

    protected AbstractPropertyChanges() {
        assert check();
    }

    protected AbstractPropertyChanges(This set) {
        super(set);

        assert check();
    }

    protected AbstractPropertyChanges(T property, PropertyChange<P> change) {
        if(!change.where.isFalse())
            add(property,change);
        
        assert check();
    }

    public boolean check() {
        for(int i=0;i<size;i++)
            assert !getValue(i).where.isFalse();
        return true;
    }

    protected <A1 extends PropertyInterface,B1 extends Property<A1>,T1 extends AbstractPropertyChanges<A1,B1,T1>,
            A2 extends PropertyInterface,B2 extends Property<A2>,T2 extends AbstractPropertyChanges<A2,B2,T2>>
        AbstractPropertyChanges(T1 changes1, T2 changes2) {
        super((QuickMap<? extends T,? extends PropertyChange<P>>) changes1);
        addAll((QuickMap<? extends T,? extends PropertyChange<P>>) changes2);

        assert check();
    }

    protected PropertyChange<P> addValue(T key, PropertyChange<P> prevValue, PropertyChange<P> newValue) {
        return prevValue.add(newValue);
    }

    public abstract This add(This add);

    protected boolean containsAll(PropertyChange<P> who, PropertyChange<P> what) {
        throw new RuntimeException("not supported yet");
    }

    public int hashValues(HashValues hashValues) {
        return MapValuesIterable.hash(this,hashValues);
    }

    public Set<Value> getValues() {
        Set<Value> result = new HashSet<Value>();
        for(int i=0;i<size;i++)
            result.addAll(getValue(i).getValues());
        return result;
    }

    public This translate(MapValuesTranslate mapValues) {
        This result = createThis();
        for(int i=0;i<size;i++)
            result.add(getKey(i),getValue(i).translate(mapValues));
        return result;
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getComponents() {
        if(components==null)
            components = AbstractMapValues.getComponents(this);
        return components;
    }

    public long getComplexity() {
        long result = 0;
        for(int i=0;i<size;i++)
            result += getValue(i).getComplexity();
        return result;
    }
    public This pack() {
        This result = createThis();
        for(int i=0;i<size;i++)
            result.add(getKey(i),getValue(i).pack());
        return result;
    }
}
