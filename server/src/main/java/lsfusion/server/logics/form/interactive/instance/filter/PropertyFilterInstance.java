package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.changed.ChangedData;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

public abstract class PropertyFilterInstance<P extends PropertyInterface> extends FilterInstance {

    protected final PropertyObjectInstance<P> property;
    public final boolean resolveAdd;

    public PropertyFilterInstance(PropertyObjectInstance<P> property, boolean resolveAdd) {
        this.property = property;
        this.resolveAdd = resolveAdd;
        this.toDraw = null;
    }
    
    private final GroupObjectInstance toDraw; // only for user filters

    public PropertyFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException, SQLHandledException {
        super(inStream,form);
        PropertyDrawInstance<P> propertyDraw = form.getPropertyDraw(inStream.readInt());
        PropertyObjectInstance<P> propertyObject = (PropertyObjectInstance<P>) propertyDraw.getValueProperty();
        if(inStream.readBoolean())
            propertyObject = propertyObject.getRemappedPropertyObject(RemoteForm.deserializeKeysValues(inStream, form));
        property = propertyObject;
        toDraw = propertyDraw.toDraw;
        resolveAdd = false;
    }

    public GroupObjectInstance getApplyObject() {
        return toDraw != null ? toDraw : property.getApplyObject();
    }

    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return property.classUpdated(gridGroups);
    }

    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return property.objectUpdated(gridGroups);
    }

    public boolean dataUpdated(ChangedData changedProps, ReallyChanged reallyChanged, Modifier modifier, boolean hidden, ImSet<GroupObjectInstance> groupObjects) throws SQLException, SQLHandledException {
        return property.dataUpdated(changedProps, reallyChanged, modifier, hidden, groupObjects);
    }

    public void fillProperties(MSet<Property> properties) {
        property.fillProperties(properties);
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

        Where changeWhere = Where.TRUE();
        for(int i=0,size=mapObjects.size();i<size;i++) {
            PropertyObjectInterfaceInstance mapObject = mapObjects.getKey(i); KeyExpr mapKey = mapObjects.getValue(i);
            Where mapWhere;
            if(mapObject.getApplyObject() != object.groupTo) {
                ObjectValue objectValue = mapObject.getObjectValue();
                if(objectValue instanceof NullValue)
                    mapWhere = Where.FALSE();
                else
                    mapWhere = mapKey.compare((DataObject)objectValue, Compare.EQUALS);
            } else // assert что тогда sibObject instanceof ObjectInstance потому как ApplyObject = null а object.groupTo !=null
                if(!mapObject.equals(object))
                    mapWhere = mapKey.isUpClass(((ObjectInstance)mapObject).getGridClass());
                else
                    mapWhere = mapKey.compare(addObject.getExpr(), Compare.EQUALS);
            changeWhere = changeWhere.and(mapWhere);
        }
        
        return changeWhere;
    }

    protected void fillObjects(MSet<ObjectInstance> objects) {
        property.fillObjects(objects);
    }
}
