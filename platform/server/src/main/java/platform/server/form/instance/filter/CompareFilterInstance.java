package platform.server.form.instance.filter;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class CompareFilterInstance<P extends PropertyInterface> extends PropertyFilterInstance<P> {

    public Compare compare;
    public CompareValue value;

    // не можем хранить ссылку на Entity, так как этот Instance может создаваться на стороне клиента и не иметь Entity
    public CompareFilterInstance(PropertyObjectInstance<P> iProperty,Compare iCompare, CompareValue iValue) {
        super(iProperty);
        compare = iCompare;
        value = iValue;
    }

    public CompareFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException {
        super(inStream,form);
        compare = Compare.deserialize(inStream);
        value = deserializeCompare(inStream, form, property.getType());
    }

    private static CompareValue deserializeCompare(DataInputStream inStream, FormInstance form, Type DBType) throws IOException, SQLException {
        byte type = inStream.readByte();
        switch(type) {
            case 0:
                return form.session.getObjectValue(BaseUtils.deserializeObject(inStream),DBType);
            case 1:
                return form.getObjectInstance(inStream.readInt());
            case 2:
                return ((PropertyDrawInstance<?>)form.getPropertyDraw(inStream.readInt())).propertyObject;
        }

        throw new IOException();
    }

    @Override
    public boolean dataUpdated(Collection<Property> changedProps) {
        return super.dataUpdated(changedProps) || value.dataUpdated(changedProps);
    }

    @Override
    public boolean classUpdated(GroupObjectInstance classGroup) {
        return super.classUpdated(classGroup) || value.classUpdated(classGroup);
    }

    @Override
    public boolean objectUpdated(Set<GroupObjectInstance> skipGroups) {
        return super.objectUpdated(skipGroups) || value.objectUpdated(skipGroups);
    }

    public Where getWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier) throws SQLException {
        return property.getExpr(mapKeys, modifier).compare(value.getExpr(mapKeys, modifier), compare);
    }

    @Override
    public void fillProperties(Set<Property> properties) {
        super.fillProperties(properties);
        value.fillProperties(properties);
    }

    @Override
    public boolean isInInterface(GroupObjectInstance classGroup) {
        return super.isInInterface(classGroup) && value.isInInterface(classGroup);
    }

    @Override
    public void resolveAdd(DataSession session, Modifier<? extends Changes> modifier, CustomObjectInstance object, DataObject addObject) throws SQLException {

        if(compare!=Compare.EQUALS)
            return;

        Map<P, KeyExpr> mapKeys = property.property.getMapKeys();
        Map<PropertyObjectInterfaceInstance, KeyExpr> mapObjects = BaseUtils.crossJoin(property.mapping, mapKeys);
        Where changeWhere = Where.TRUE;
        Where mapWhere;
        for(Map.Entry<PropertyObjectInterfaceInstance, KeyExpr> mapObject : mapObjects.entrySet()) {
            if(mapObject.getKey().getApplyObject() !=object.groupTo)
                mapWhere = new EqualsWhere(mapObject.getValue(), mapObject.getKey().getDataObject().getExpr());
            else // assert что тогда sibObject instanceof ObjectInstance потому как ApplyObject = null а object.groupTo !=null
                if(!mapObject.getKey().equals(object))
                    mapWhere = mapObject.getValue().isClass(((ObjectInstance)mapObject.getKey()).getGridClass().getUpSet());
                else
                    mapWhere = new EqualsWhere(mapObject.getValue(),addObject.getExpr());
            changeWhere = changeWhere.and(mapWhere);
        }
        session.execute(property.property, new PropertyChange<P>(mapKeys,
                            value.getExpr(BaseUtils.filterKeys(mapObjects, object.groupTo.objects), modifier), changeWhere),
                            modifier, null, null);
    }
}
