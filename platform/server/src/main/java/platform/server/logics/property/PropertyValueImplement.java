package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.CustomClass;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;
import platform.server.form.instance.remote.RemoteForm;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyValueImplement<P extends PropertyInterface> extends PropertyImplement<DataObject,P> {

    public PropertyValueImplement(Property<P> property, Map<P, DataObject> mapping) {
        super(property, mapping);
    }

    public Object read(SQLSession session, Modifier<? extends Changes> modifier) throws SQLException {
        return property.read(session, mapping, modifier);
    }

    @Override
    public String toString() {
        return property.toString();
    }

    public int getID() {
        return property.ID;
    }

    public CustomClass getDialogClass(DataSession session) {
        return property.getDialogClass(mapping, session.getCurrentClasses(mapping));
    }

    public PropertyChange<P> getPropertyChange(Expr expr, Modifier<? extends Changes> modifier, Map<P, PropertyObjectInterfaceInstance> mapObjects, GroupObjectInstance groupObject) throws SQLException {
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
        Map<P, KeyExpr> filteredMapKeys = mapKeys;
        // все кто ObjectInstance и в переданном groupObject, KeyExpr'ы остальным mapping
        Map<P, ObjectInstance> groupChange = new HashMap<P, ObjectInstance>();
        if (groupObject != null) {
            for (Map.Entry<P, PropertyObjectInterfaceInstance> mapObject : mapObjects.entrySet())
                if (mapObject.getValue() instanceof ObjectInstance && ((ObjectInstance) mapObject.getValue()).groupTo == groupObject)
                    groupChange.put(mapObject.getKey(), (ObjectInstance) mapObject.getValue());
            filteredMapKeys = BaseUtils.filterNotKeys(mapKeys, groupChange.keySet());
        }

        return new PropertyChange<P>(
                mapKeys,
                expr,
                CompareWhere.compareValues(
                    filteredMapKeys,
                    mapping
                ).and(
                    groupObject == null
                    ? Where.TRUE
                    : groupObject.getWhere(BaseUtils.crossJoin(groupChange, mapKeys), modifier)
                )
        );
    }

    public boolean canBeChanged(Modifier<? extends Changes> modifier) throws SQLException {
        return !property.getDataChanges(getPropertyChange(property.changeExpr, modifier, null, null), null, modifier).changes.isEmpty();
    }

    public List<ClientAction> execute(DataSession session, Object value, Modifier<? extends Changes> modifier, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects, GroupObjectInstance groupObject) throws SQLException {
        return session.execute(property, getPropertyChange(session.getObjectValue(value, property.getType()).getExpr(), modifier, mapObjects, groupObject), modifier, executeForm, mapObjects);
    }
}
