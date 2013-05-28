package platform.server.form.instance.filter;

import platform.base.FunctionSet;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.interop.Compare;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;

import java.io.DataInputStream;
import java.io.IOException;

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

    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return property.classUpdated(gridGroups);
    }

    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return property.objectUpdated(gridGroups);
    }

    public boolean dataUpdated(FunctionSet<CalcProperty> changedProps) {
        return property.dataUpdated(changedProps);
    }

    public void fillProperties(MSet<CalcProperty> properties) {
        property.fillProperties(properties);
    }

    public boolean isInInterface(GroupObjectInstance classGroup) {
        return property.isInInterface(classGroup);
    }
    
    protected boolean hasObjectInInterface(CustomObjectInstance object) {
        // проверка на то, что в фильтре есть в качестве ключа свойства нужный ObjectInstance
        boolean inInterface = false;
        for (PropertyObjectInterfaceInstance interfaceInstance : property.mapping.valueIt()) {
            if (interfaceInstance == object) {
                inInterface = true;
                break;
            }
        }
        return inInterface;
    }
    
    protected Where getChangedWhere(CustomObjectInstance object, ImMap<PropertyObjectInterfaceInstance, KeyExpr> mapObjects, DataObject addObject) {

        Where changeWhere = Where.TRUE;
        for(int i=0,size=mapObjects.size();i<size;i++) {
            PropertyObjectInterfaceInstance mapObject = mapObjects.getKey(i); KeyExpr mapKey = mapObjects.getValue(i);
            Where mapWhere;
            if(mapObject.getApplyObject() != object.groupTo)
                mapWhere = mapKey.compare(mapObject.getDataObject(), Compare.EQUALS);
            else // assert что тогда sibObject instanceof ObjectInstance потому как ApplyObject = null а object.groupTo !=null
                if(!mapObject.equals(object))
                    mapWhere = mapKey.isUpClass(((ObjectInstance)mapObject).getGridClass());
                else
                    mapWhere = mapKey.compare(addObject.getExpr(), Compare.EQUALS);
            changeWhere = changeWhere.and(mapWhere);
        }
        
        return changeWhere;
    }
}
