package platform.server.session;

import platform.server.caches.MapValues;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.logics.property.DataProperty;

import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;

public class SimpleChanges extends Changes<SimpleChanges> {

    public SimpleChanges() {
    }
    public static final SimpleChanges EMPTY = new SimpleChanges();

    public SimpleChanges(SimpleChanges changes, Changes merge) {
        super(changes, merge, true);
    }
    public SimpleChanges addChanges(Changes changes) {
        return new SimpleChanges(this, changes);
    }

    protected SimpleChanges(SimpleChanges session) {
        super(session);
    }

    public SimpleChanges(Modifier<SimpleChanges> modifier) {
        super(modifier);
    }

    public SimpleChanges(SimpleChanges changes, SimpleChanges merge) {
        super(changes, merge);
    }
    public SimpleChanges add(SimpleChanges changes) {
        return new SimpleChanges(this, changes);
    }

    public SimpleChanges(Map<CustomClass, MapValues> add, Map<CustomClass, MapValues> remove, Map<DataProperty, MapValues> data, MapValues newClasses) {
        super(add, remove, data, newClasses);
    }

    public SimpleChanges(Changes<SimpleChanges> changes, MapValuesTranslate mapValues) {
        super(changes, mapValues);
    }
    public SimpleChanges translate(MapValuesTranslate mapValues) {
        return new SimpleChanges(this, mapValues);
    }

    public SimpleChanges(SimpleChanges changes, Collection<ValueClass> valueClasses, boolean onlyRemove) {
        this(changes, onlyRemove?new ArrayList<ValueClass>():valueClasses, valueClasses, false);
    }

    public SimpleChanges(SimpleChanges changes, Collection<? extends ValueClass> addClasses, Collection<? extends ValueClass> removeClasses, boolean classExpr) {
        this();

        MapValues addTable;
        for(ValueClass valueClass : addClasses)
            if(valueClass instanceof CustomClass && ((addTable = changes.add.get((CustomClass)valueClass))!=null))
                add.put((CustomClass) valueClass,addTable);

        MapValues removeTable;
        for(ValueClass valueClass : removeClasses)
            if(valueClass instanceof CustomClass && ((removeTable = changes.remove.get((CustomClass)valueClass))!=null))
                remove.put((CustomClass) valueClass,removeTable);

        if(classExpr)
            newClasses = changes.newClasses;
    }

    public SimpleChanges(SimpleChanges changes, boolean classExpr) {
        this(changes, new ArrayList<ValueClass>(), new ArrayList<ValueClass>(), classExpr);
    }

    public SimpleChanges(SimpleChanges changes, DataProperty property) {
        this();

        MapValues dataChange = changes.data.get(property);
        if(dataChange!=null)
            data.put(property, dataChange);
    }

    public SimpleChanges(SimpleChanges changes, MapValues update) {
        this();

        for(Map.Entry<CustomClass,MapValues> addEntry : changes.add.entrySet())
            add.put(addEntry.getKey(), update);

        for(Map.Entry<CustomClass,MapValues> removeEntry : changes.remove.entrySet())
            remove.put(removeEntry.getKey(), update);

        for(Map.Entry<DataProperty,MapValues> dataEntry : changes.data.entrySet())
            data.put(dataEntry.getKey(), update);

        if(changes.newClasses!=null)
            newClasses = update;
    }

}
