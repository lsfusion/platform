package platform.server.session;

import platform.base.BaseUtils;
import platform.server.caches.ManualLazy;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.UserProperty;

import java.util.HashMap;
import java.util.Map;

public class MapDataChanges<P extends PropertyInterface> {

    public final DataChanges changes;
    public final Map<UserProperty, Map<ClassPropertyInterface, P>> map;

    public MapDataChanges(DataChanges changes, Map<UserProperty, Map<ClassPropertyInterface, P>> map) {
        this.changes = changes;
        this.map = map;
    }

    public MapDataChanges() {
        this(new DataChanges());
    }

    public MapDataChanges(DataChanges changes) {
        this.changes = changes;
        map = new HashMap<UserProperty, Map<ClassPropertyInterface,P>>();
    }

    private MapDataChanges(MapDataChanges<P> mapChanges) {
        this.changes = mapChanges.changes.pack();
        this.map = mapChanges.map;
    }

    private Long complexity;
    @ManualLazy
    public long getComplexity() {
        if(complexity==null)
            complexity = changes.getComplexity();
        return complexity;
    }

    public MapDataChanges<P> pack() {
        return new MapDataChanges<P>(this);
    }

    public <T extends PropertyInterface> MapDataChanges<T> map(Map<P,T> interfaceMap) {
        Map<UserProperty, Map<ClassPropertyInterface,T>> transMap = new HashMap<UserProperty, Map<ClassPropertyInterface,T>>();
        for(Map.Entry<UserProperty,Map<ClassPropertyInterface,P>> entry : map.entrySet())
            transMap.put(entry.getKey(), BaseUtils.join(entry.getValue(),interfaceMap));
        return new MapDataChanges<T>(changes, transMap);
    }

    private MapDataChanges(MapDataChanges<P> op1, MapDataChanges<P> op2) {
        changes = op1.changes.add(op2.changes);

        map = new HashMap<UserProperty, Map<ClassPropertyInterface,P>>(op1.map);
        for(Map.Entry<UserProperty,Map<ClassPropertyInterface,P>> entry : op2.map.entrySet()) {
            Map<ClassPropertyInterface, P> existMap = map.get(entry.getKey());
            if(existMap==null)
                map.put(entry.getKey(),entry.getValue());
            else
                map.put(entry.getKey(),BaseUtils.merge(existMap,entry.getValue()));
        }
    }
    public MapDataChanges<P> add(MapDataChanges<P> add) {
        return new MapDataChanges<P>(this, add);
    }

    private MapDataChanges(MapDataChanges<P> trans, MapValuesTranslate mapValues) {
        changes = trans.changes.translate(mapValues);
        map = trans.map;
    }
    public MapDataChanges<P> translate(MapValuesTranslate mapValues) {
        return new MapDataChanges<P>(this, mapValues);
    }
}
