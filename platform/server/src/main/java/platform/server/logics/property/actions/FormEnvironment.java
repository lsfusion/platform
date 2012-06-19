package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.CalcPropertyInterfaceImplement;

import java.util.Map;

import static platform.base.BaseUtils.nullInnerJoin;
import static platform.base.BaseUtils.nullJoin;

public class FormEnvironment<P extends PropertyInterface> {
    private final Map<P, PropertyObjectInterfaceInstance> mapObjects;
    private final PropertyDrawInstance drawInstance;

    public FormEnvironment(Map<P, PropertyObjectInterfaceInstance> mapObjects, PropertyDrawInstance drawInstance) {
        this.mapObjects = mapObjects;
        this.drawInstance = drawInstance;
    }

    public Map<P, PropertyObjectInterfaceInstance> getMapObjects() {
        return mapObjects;
    }
    
    public <T extends PropertyInterface> FormEnvironment<T> mapJoin(Map<T, ? extends CalcPropertyInterfaceImplement<P>> map) {
        return new FormEnvironment<T>(nullInnerJoin(map, mapObjects), drawInstance);
    }

    public <T extends PropertyInterface> FormEnvironment<T> map(Map<T, P> map) {
        return new FormEnvironment<T>(nullInnerJoin(map, mapObjects), drawInstance);
    }

    public PropertyDrawInstance getDrawInstance() {
        return drawInstance;
    }
}
