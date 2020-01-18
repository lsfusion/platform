package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.NotNullFilterInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.interactive.property.checked.PullChangeProperty;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ContextFilterInstance<P extends PropertyInterface> {

    private final Property<P> property;

    private final ImMap<P, ? extends ObjectValue> mapValues; // external context
    private final ImRevMap<P, ObjectEntity> mapObjects; // objects
    
    public ContextFilterInstance(Property<P> property, ImMap<P, ? extends ObjectValue> mapValues, ImRevMap<P, ObjectEntity> mapObjects) {
        this.property = property;
        this.mapValues = mapValues;
        this.mapObjects = mapObjects;
    }

    public FilterInstance getFilter(InstanceFactory factory) {
        return new NotNullFilterInstance<>(
                new PropertyObjectInstance<>(property, 
                        MapFact.<P, PropertyObjectInterfaceInstance>addExcl(mapValues, mapObjects.mapValues(entity -> factory.getInstance(entity)))));
    }

    public static ImSet<PullChangeProperty> getPullProps(ImSet<ContextFilterInstance> filters) {
        return filters.filterFn(element -> element.property instanceof PullChangeProperty).mapSetValues(value -> ((PullChangeProperty)value.property));        
    }
}
