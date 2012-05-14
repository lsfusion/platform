package platform.server.form.instance.filter;

import platform.interop.Compare;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class PropertyFilterInstance<P extends PropertyInterface> extends FilterInstance {

    public CalcPropertyObjectInstance<P> property;
    public final boolean resolveAdd;

    public PropertyFilterInstance(CalcPropertyObjectInstance<P> property, boolean resolveAdd) {
        this.property = property;
        this.resolveAdd = resolveAdd;
    }

    public PropertyFilterInstance(DataInputStream inStream, FormInstance form) throws IOException {
        super(inStream,form);
        property = (CalcPropertyObjectInstance<P>) ((PropertyDrawInstance<P>)form.getPropertyDraw(inStream.readInt())).propertyObject;
        resolveAdd = false;
    }

    public GroupObjectInstance getApplyObject() {
        return property.getApplyObject();
    }

    public boolean classUpdated(Set<GroupObjectInstance> gridGroups) {
        return property.classUpdated(gridGroups);
    }

    public boolean objectUpdated(Set<GroupObjectInstance> gridGroups) {
        return property.objectUpdated(gridGroups);
    }

    public boolean dataUpdated(Collection<CalcProperty> changedProps) {
        return property.dataUpdated(changedProps);
    }

    public void fillProperties(Set<CalcProperty> properties) {
        property.fillProperties(properties);
    }

    public boolean isInInterface(GroupObjectInstance classGroup) {
        return property.isInInterface(classGroup);
    }
    
    protected boolean hasObjectInInterface(CustomObjectInstance object) {
        // проверка на то, что в фильтре есть в качестве ключа свойства нужный ObjectInstance
        boolean inInterface = false;
        for (PropertyObjectInterfaceInstance interfaceInstance : property.mapping.values()) {
            if (interfaceInstance == object) {
                inInterface = true;
                break;
            }
        }
        return inInterface;
    }
    
    protected Where getChangedWhere(CustomObjectInstance object, Map<PropertyObjectInterfaceInstance, KeyExpr> mapObjects, DataObject addObject) {

        Where changeWhere = Where.TRUE;
        Where mapWhere;
        for(Map.Entry<PropertyObjectInterfaceInstance, KeyExpr> mapObject : mapObjects.entrySet()) {
            if(mapObject.getKey().getApplyObject() != object.groupTo)
                mapWhere = mapObject.getValue().compare(mapObject.getKey().getDataObject(), Compare.EQUALS);
            else // assert что тогда sibObject instanceof ObjectInstance потому как ApplyObject = null а object.groupTo !=null
                if(!mapObject.getKey().equals(object))
                    mapWhere = mapObject.getValue().isClass(((ObjectInstance)mapObject.getKey()).getGridClass().getUpSet());
                else
                    mapWhere = mapObject.getValue().compare(addObject.getExpr(), Compare.EQUALS);
            changeWhere = changeWhere.and(mapWhere);
        }
        
        return changeWhere;
    }
}
