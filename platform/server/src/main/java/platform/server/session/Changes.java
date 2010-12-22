package platform.server.session;

import platform.base.BaseUtils;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValues;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.CustomClass;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.logics.property.DataProperty;

import java.util.*;

public abstract class Changes<U extends Changes<U>> extends AbstractMapValues<U> {

    public final Map<CustomClass, MapValues> add;
    public final Map<CustomClass, MapValues> remove;
    public final Map<DataProperty, MapValues> data;

    public MapValues newClasses; //final на самом деле, но как в add и remove так как используется this() в явную это не задается

    public boolean hasChanges() { // newClasses - не нужен так как иначе либо add либо remove непустые
        return !add.isEmpty() || !remove.isEmpty() || !data.isEmpty() || modifyUsed();
    }

    public Changes() {
        this(new HashMap<CustomClass, MapValues>(), new HashMap<CustomClass, MapValues>(), new HashMap<DataProperty, MapValues>(), null);
    }

    protected Changes(Changes<U> changes, MapValuesTranslate mapValues) {
        this(mapValues.translateValues(changes.add), mapValues.translateValues(changes.remove), mapValues.translateValues(changes.data), changes.newClasses == null ? null : changes.newClasses.translate(mapValues));
    }

    protected Changes(Map<CustomClass, MapValues> add, Map<CustomClass, MapValues> remove, Map<DataProperty, MapValues> data, MapValues newClasses) {
        this.add = add;
        this.remove = remove;
        this.data = data;
        this.newClasses = newClasses;
    }

    public Changes(Changes<?> session) { // можно так как SessionChanges Immutable
        this(session.add, session.remove, session.data, session.newClasses);
    }

    public Changes(Modifier<U> modifier) { // можно так как SessionChanges Immutable
        this(modifier.getChanges());
    }

    // весь этот огород, для того чтобы если даже разные классы, но нету изменений, все равно давать equals и использовать одни кэши
    public boolean modifyUsed() {
        return false;
    }

    protected boolean modifyEquals(U changes) {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Changes && add.equals(((Changes) o).add) && remove.equals(((Changes) o).remove)  && data.equals(((Changes) o).data) && BaseUtils.nullEquals(newClasses, ((Changes) o).newClasses))) return false;

        if(getClass()==o.getClass())
            return modifyEquals((U)o);

        return !modifyUsed() && !((Changes)o).modifyUsed();
    }

    protected Changes(U changes, Changes<?> merge, boolean cast) {
        add = BaseUtils.merge(changes.add, merge.add);
        remove = BaseUtils.merge(changes.remove, merge.remove);
        data = BaseUtils.merge(changes.data, merge.data);

        newClasses = BaseUtils.nvl(changes.newClasses, merge.newClasses);
    }
    public abstract U addChanges(Changes changes);

    protected Changes(U changes, U merge) {
        this(changes, merge, true);
    }
    public abstract U add(U changes);

    @IdentityLazy
    public int hashValues(HashValues hashValues) {
        return ((MapValuesIterable.hash(add,hashValues) * 31 + MapValuesIterable.hash(remove,hashValues)) * 31 + MapValuesIterable.hash(data,hashValues)) * 31 + (newClasses ==null?0: newClasses.hashValues(hashValues));
    }

    @IdentityLazy
    public Set<Value> getValues() {
        Set<Value> result = new HashSet<Value>();
        MapValuesIterable.enumValues(result,add);
        MapValuesIterable.enumValues(result,remove);
        MapValuesIterable.enumValues(result,data);
        if(newClasses !=null)
            result.addAll(newClasses.getValues());
        return result;
    }
}
