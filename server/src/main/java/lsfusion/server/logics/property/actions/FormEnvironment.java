package lsfusion.server.logics.property.actions;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.form.instance.PropertyDrawInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;

public class FormEnvironment<P extends PropertyInterface> {
    private final ImMap<P, PropertyObjectInterfaceInstance> mapObjects;
    private final PropertyDrawInstance drawInstance;

    public FormEnvironment(ImMap<P, PropertyObjectInterfaceInstance> mapObjects, PropertyDrawInstance drawInstance) {
        this.mapObjects = mapObjects;
        this.drawInstance = drawInstance;
    }

    public ImMap<P, PropertyObjectInterfaceInstance> getMapObjects() {
        return mapObjects;
    }
    
    public <T extends PropertyInterface> FormEnvironment<T> mapJoin(ImMap<T, ? extends CalcPropertyInterfaceImplement<P>> map) {
        return new FormEnvironment<T>(MapFact.nullInnerJoin(map, mapObjects), drawInstance);
    }

    public <T extends PropertyInterface> FormEnvironment<T> map(ImRevMap<T, P> map) {
        return new FormEnvironment<T>(MapFact.nullInnerJoin(map, mapObjects), drawInstance);
    }

    public PropertyDrawInstance getDrawInstance() {
        return drawInstance;
    }
}
