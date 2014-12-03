package lsfusion.server.form.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.Compare;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.instance.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

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

    public boolean dataUpdated(ChangedData changedProps, ReallyChanged reallyChanged, Modifier modifier) throws SQLException, SQLHandledException {
        return property.dataUpdated(changedProps, reallyChanged, modifier);
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
            if(mapObject.getApplyObject() != object.groupTo) {
                if(mapObject.isNull())
                    mapWhere = Where.FALSE;
                else
                    mapWhere = mapKey.compare(mapObject.getDataObject(), Compare.EQUALS);
            } else // assert что тогда sibObject instanceof ObjectInstance потому как ApplyObject = null а object.groupTo !=null
                if(!mapObject.equals(object))
                    mapWhere = mapKey.isUpClass(((ObjectInstance)mapObject).getGridClass());
                else
                    mapWhere = mapKey.compare(addObject.getExpr(), Compare.EQUALS);
            changeWhere = changeWhere.and(mapWhere);
        }
        
        return changeWhere;
    }
}
