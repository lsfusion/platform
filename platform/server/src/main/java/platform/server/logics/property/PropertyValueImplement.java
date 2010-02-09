package platform.server.logics.property;

import platform.server.logics.DataObject;
import platform.server.data.SQLSession;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CompareWhere;
import platform.server.session.*;
import platform.server.classes.CustomClass;

import java.util.Map;
import java.sql.SQLException;

public class PropertyValueImplement<P extends PropertyInterface> extends PropertyImplement<DataObject,P> {

    public PropertyValueImplement(Property<P> property, Map<P, DataObject> mapping) {
        super(property, mapping);
    }

    public Object read(SQLSession session, TableModifier<? extends TableChanges> modifier) throws SQLException {
        return property.read(session, mapping, modifier);
    }

    @Override
    public String toString() {
        return property.toString();
    }

    public int getID() {
        return property.ID;
    }

    public CustomClass getDialogClass() {
        return (CustomClass) property.getValueClass();
    }

    public DataChanges getDataChanges(Expr expr, TableModifier<? extends TableChanges> modifier) {
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
        return property.getDataChanges(new PropertyChange<P>(mapKeys, expr, CompareWhere.compareValues(mapKeys, mapping)), null, modifier);
    }
    
    public boolean canBeChanged(TableModifier<? extends TableChanges> modifier) {
        return getDataChanges(property.changeExpr,modifier).hasChanges();
    }

    public void change(DataSession session, TableModifier<? extends TableChanges> modifier,Object value) throws SQLException {
        getDataChanges(session.getObjectValue(value, property.getType()).getExpr(),modifier).change(session);
    }

}
