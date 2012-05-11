package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.PropertyInterfaceImplement;

import java.util.Map;

import static platform.base.BaseUtils.nullInnerJoin;
import static platform.base.BaseUtils.nullJoin;

public class FormEnvironment {
    private final Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects;
    private final PropertyDrawInstance drawInstance;

    public FormEnvironment(Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, PropertyDrawInstance drawInstance) {
        this.mapObjects = mapObjects;
        this.drawInstance = drawInstance;
    }

    public Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> getMapObjects() {
        return mapObjects;
    }
    
    public FormEnvironment map(Map<ClassPropertyInterface, ? extends PropertyInterfaceImplement<ClassPropertyInterface>> map) {
        return new FormEnvironment(nullInnerJoin(map, mapObjects), drawInstance);
    }

    public PropertyDrawInstance getDrawInstance() {
        return drawInstance;
    }
}
