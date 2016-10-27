package lsfusion.server.logics.property.actions;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyDrawInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;

public class FormEnvironment<P extends PropertyInterface> {
    private final ImMap<P, PropertyObjectInterfaceInstance> mapObjects;
    private final PropertyDrawInstance changingDrawInstance;
    private final FormInstance formInstance;

    public FormEnvironment(ImMap<P, PropertyObjectInterfaceInstance> mapObjects, PropertyDrawInstance changingDrawInstance, FormInstance formInstance) {
        this.mapObjects = mapObjects;
        this.changingDrawInstance = changingDrawInstance;
        this.formInstance = formInstance;
    }

    public ImMap<P, PropertyObjectInterfaceInstance> getMapObjects() {
        return mapObjects;
    }
    
    public <T extends PropertyInterface> FormEnvironment<T> mapJoin(ImMap<T, ? extends CalcPropertyInterfaceImplement<P>> map) {
        return new FormEnvironment<>(MapFact.nullInnerJoin(map, mapObjects), changingDrawInstance, formInstance);
    }

    public <T extends PropertyInterface> FormEnvironment<T> map(ImRevMap<T, P> map) {
        return new FormEnvironment<>(MapFact.nullInnerJoin(map, mapObjects), changingDrawInstance, formInstance);
    }

    public PropertyDrawInstance getChangingDrawInstance() {
        return changingDrawInstance;
    }
    
    public FormInstance getInstance() {
        return formInstance;
    }
}
