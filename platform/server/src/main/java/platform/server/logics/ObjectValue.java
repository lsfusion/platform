package platform.server.logics;

import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.property.Property;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.filter.CompareValue;
import platform.server.data.where.Where;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public abstract class ObjectValue implements CompareValue {

    public abstract String getString(SQLSyntax syntax);

    public abstract boolean isString(SQLSyntax syntax);

    public abstract Expr getExpr();

    public abstract Object getValue();

    public static ObjectValue getValue(Object value, ConcreteClass objectClass) {
        if(value==null)
            return NullValue.instance;
        else
            return new DataObject(value, objectClass);
    }

    public Expr getExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends Expr> classSource, Modifier<? extends Changes> modifier) throws SQLException {
        return getExpr();
    }

    public static <K> Map<K,Expr> getMapExprs(Map<K,? extends ObjectValue> map) {
        Map<K,Expr> mapExprs = new HashMap<K,Expr>();
        for(Map.Entry<K,? extends ObjectValue> keyField : map.entrySet())
            mapExprs.put(keyField.getKey(), keyField.getValue().getExpr());
        return mapExprs;
    }

    
    public boolean classUpdated(GroupObjectImplement classGroup) {return false;}
    public boolean objectUpdated(GroupObjectImplement classGroup) {return false;}
    public boolean dataUpdated(Collection<Property> changedProps) {return false;}
    public void fillProperties(Set<Property> properties) {}
    public boolean isInInterface(GroupObjectImplement classGroup) {return true;}

    public abstract Where order(Expr expr, boolean desc, Where orderWhere);

}
