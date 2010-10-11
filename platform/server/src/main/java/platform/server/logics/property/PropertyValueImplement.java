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

    @Override
    public String toString() {
        return property.toString();
    }

    public int getID() {
        return property.ID;
    }

    public PropertyChange<P> getPropertyChange(Expr expr) throws SQLException {
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
        return new PropertyChange<P>(mapKeys,expr,CompareWhere.compareValues(mapKeys,mapping));
    }

    public boolean canBeChanged(Modifier<? extends Changes> modifier) throws SQLException {
        return !property.getDataChanges(getPropertyChange(property.changeExpr), null, modifier).changes.isEmpty();
    }

}
