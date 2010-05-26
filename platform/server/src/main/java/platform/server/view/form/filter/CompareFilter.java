package platform.server.view.form.filter;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.type.Type;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.DataProperty;
import platform.server.logics.DataObject;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;
import platform.server.session.DataSession;
import platform.server.view.form.*;
import platform.server.data.where.Where;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class CompareFilter<P extends PropertyInterface> extends PropertyFilter<P> {

    public Compare compare;
    public CompareValue value;

    public CompareFilter(PropertyObjectImplement<P> iProperty,Compare iCompare, CompareValue iValue) {
        super(iProperty);
        compare = iCompare;
        value = iValue;
    }

    public CompareFilter(DataInputStream inStream, RemoteForm form) throws IOException, SQLException {
        super(inStream,form);
        compare = Compare.deserialize(inStream);
        value = deserializeCompare(inStream, form, property.getType());
    }

    private static CompareValue deserializeCompare(DataInputStream inStream, RemoteForm form, Type DBType) throws IOException, SQLException {
        byte type = inStream.readByte();
        switch(type) {
            case 0:
                return form.session.getObjectValue(BaseUtils.deserializeObject(inStream),DBType);
            case 1:
                return form.getObjectImplement(inStream.readInt());
            case 2:
                return ((PropertyView<?>)form.getPropertyView(inStream.readInt())).view;
        }

        throw new IOException();
    }

    @Override
    public boolean dataUpdated(Collection<Property> changedProps) {
        return super.dataUpdated(changedProps) || value.dataUpdated(changedProps);
    }

    @Override
    public boolean classUpdated(GroupObjectImplement classGroup) {
        return super.classUpdated(classGroup) || value.classUpdated(classGroup);
    }

    @Override
    public boolean objectUpdated(GroupObjectImplement classGroup) {
        return super.objectUpdated(classGroup) || value.objectUpdated(classGroup);
    }

    public Where getWhere(Map<ObjectImplement, ? extends Expr> mapKeys, Set<GroupObjectImplement> classGroup, Modifier<? extends Changes> modifier) throws SQLException {
        return property.getExpr(classGroup, mapKeys, modifier).compare(value.getExpr(classGroup, mapKeys, modifier), compare);
    }

    @Override
    public void fillProperties(Set<Property> properties) {
        super.fillProperties(properties);
        value.fillProperties(properties);
    }

    @Override
    public boolean isInInterface(GroupObjectImplement classGroup) {
        return super.isInInterface(classGroup) && value.isInInterface(classGroup);
    }

    @Override
    public boolean resolveAdd(DataSession session, Modifier<? extends Changes> modifier, CustomObjectImplement object, DataObject addObject) throws SQLException {

        if(compare!=Compare.EQUALS)
            return false;

        Map<P, KeyExpr> mapKeys = property.property.getMapKeys();
        Map<PropertyObjectInterface, KeyExpr> mapObjects = BaseUtils.crossJoin(property.mapping, mapKeys);
        Where changeWhere = Where.TRUE;
        Where mapWhere;
        for(Map.Entry<PropertyObjectInterface, KeyExpr> mapObject : mapObjects.entrySet()) {
            if(mapObject.getKey().getApplyObject() !=object.groupTo)
                mapWhere = new EqualsWhere(mapObject.getValue(), mapObject.getKey().getDataObject().getExpr());
            else // assert что тогда sibObject instanceof ObjectImplement потому как ApplyObject = null а object.groupTo !=null
                if(!mapObject.getKey().equals(object))
                    mapWhere = mapObject.getValue().isClass(((ObjectImplement)mapObject.getKey()).getGridClass().getUpSet());
                else
                    mapWhere = new EqualsWhere(mapObject.getValue(),addObject.getExpr());
            changeWhere = changeWhere.and(mapWhere);
        }
        return session.execute(property.property, new PropertyChange<P>(mapKeys,
                            value.getExpr(object.groupTo.getClassGroup(), BaseUtils.filterKeys(mapObjects, property.getObjectImplements()), modifier), changeWhere),
                            modifier, null, null);
    }
}
