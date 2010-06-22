package platform.server.logics.property;

import platform.interop.action.ClientAction;
import platform.server.classes.CustomClass;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.CompareWhere;
import platform.server.logics.DataObject;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;
import platform.server.view.form.PropertyObjectInterface;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.client.RemoteFormView;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

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

    private Map<ObjectImplement, KeyExpr> mapInterfaces(Map<P, KeyExpr> mapKeys, Map<P, PropertyObjectInterface> mapObjects) {
        Map<ObjectImplement, KeyExpr> result = new HashMap<ObjectImplement, KeyExpr>();
        for (Map.Entry<P, KeyExpr> entry : mapKeys.entrySet()) {
            PropertyObjectInterface object = mapObjects.get(entry.getKey());
            if (object instanceof ObjectImplement)
                result.put((ObjectImplement) object, entry.getValue());
        }
        return result;
    }

    public PropertyChange<P> getPropertyChange(Expr expr, Modifier<? extends Changes> modifier, Map<P, PropertyObjectInterface> mapObjects, GroupObjectImplement groupObject) throws SQLException {
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
        return new PropertyChange<P>(mapKeys, expr, (groupObject == null ) ? CompareWhere.compareValues(mapKeys, mapping) : groupObject.getWhere(mapInterfaces(mapKeys, mapObjects), groupObject.getClassGroup(), modifier));
    }

    public boolean canBeChanged(Modifier<? extends Changes> modifier) throws SQLException {
        return !property.getDataChanges(getPropertyChange(property.changeExpr, modifier, null, null), null, modifier).changes.isEmpty();
    }

    public List<ClientAction> execute(DataSession session, Object value, Modifier<? extends Changes> modifier, RemoteFormView executeForm, Map<P, PropertyObjectInterface> mapObjects, GroupObjectImplement groupObject) throws SQLException {
        return session.execute(property, getPropertyChange(session.getObjectValue(value, property.getType()).getExpr(), modifier, mapObjects, groupObject), modifier, executeForm, mapObjects);
    }
}
