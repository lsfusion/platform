package platform.server.session;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.Property;
import platform.server.caches.MapValues;
import platform.server.caches.hash.HashValues;
import platform.server.caches.MapValuesIterable;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.KeyTranslator;
import platform.base.QuickMap;

import java.util.*;

public abstract class AbstractPropertyChanges<P extends PropertyInterface, T extends Property<P>, This extends AbstractPropertyChanges<P,T,This>> extends QuickMap<T, PropertyChange<P>> implements MapValues<This> {

    protected abstract This createThis();

    protected AbstractPropertyChanges() {
    }

    protected AbstractPropertyChanges(T property, PropertyChange<P> change) {
        super(property, change);
    }

    protected <A1 extends PropertyInterface,B1 extends Property<A1>,T1 extends AbstractPropertyChanges<A1,B1,T1>,
            A2 extends PropertyInterface,B2 extends Property<A2>,T2 extends AbstractPropertyChanges<A2,B2,T2>>
        AbstractPropertyChanges(T1 changes1, T2 changes2) {
        super((QuickMap<? extends T,? extends PropertyChange<P>>) changes1);
        addAll((QuickMap<? extends T,? extends PropertyChange<P>>) changes2);
    }

    protected PropertyChange<P> addValue(PropertyChange<P> prevValue, PropertyChange<P> newValue) {
        return prevValue.add(newValue);
    }

    public abstract This add(This add);

    protected boolean containsAll(PropertyChange<P> who, PropertyChange<P> what) {
        throw new RuntimeException("not supported yet");
    }

    public boolean hasChanges() {
        for(int i=0;i<size;i++)
            if(!getValue(i).where.isFalse())
                return true;
        return false;
    }

    public Collection<T> getProperties() {
        Collection<T> result = new ArrayList<T>();
        for(int i=0;i<size;i++)
            if(!getValue(i).where.isFalse())
                result.add(getKey(i));
        return result;
    }

    public int hashValues(HashValues hashValues) {
        return MapValuesIterable.hash(this,hashValues);
    }

    public Set<ValueExpr> getValues() {
        Set<ValueExpr> result = new HashSet<ValueExpr>();
        for(int i=0;i<size;i++)
            result.addAll(getValue(i).getValues());
        return result;
    }

    public This translate(Map<ValueExpr, ValueExpr> mapValues) {
        This result = createThis();
        for(int i=0;i<size;i++)
            result.add(getKey(i),getValue(i).translate(mapValues));
        return result;
    }

    public This translate(KeyTranslator translator) {
        This result = createThis();
        for(int i=0;i<size;i++)
            result.add(getKey(i),getValue(i).translate(translator));
        return result;
    }

}
