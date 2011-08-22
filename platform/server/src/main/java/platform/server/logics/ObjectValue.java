package platform.server.logics;

import platform.server.caches.AbstractMapValues;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.where.Where;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.filter.CompareValue;
import platform.server.logics.property.Property;
import platform.server.session.Changes;
import platform.server.session.SessionChanges;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class ObjectValue<T extends ObjectValue<T>> extends AbstractMapValues<T> implements CompareValue {

    public abstract String getString(SQLSyntax syntax);

    public abstract boolean isString(SQLSyntax syntax);

    public abstract Expr getExpr();
    public abstract Expr getStaticExpr();

    public abstract Object getValue();

    public static ObjectValue getValue(Object value, ConcreteClass objectClass) {
        if(value==null)
            return NullValue.instance;
        else
            return new DataObject(value, objectClass);
    }

    public Expr getExpr(Map<ObjectInstance, ? extends Expr> classSource, Modifier<? extends Changes> modifier) {
        return getExpr();
    }

    public static <K> Map<K,Expr> getMapExprs(Map<K,? extends ObjectValue> map) {
        Map<K,Expr> mapExprs = new HashMap<K,Expr>();
        for(Map.Entry<K,? extends ObjectValue> keyField : map.entrySet())
            mapExprs.put(keyField.getKey(), keyField.getValue().getExpr());
        return mapExprs;
    }

    public static <K> boolean containsNull(Collection<ObjectValue> col) {
        for(ObjectValue value : col)
            if(value instanceof NullValue)
                return true;
        return false;
    }

    
    public boolean classUpdated(Set<GroupObjectInstance> gridGroups) {return false;}
    public boolean objectUpdated(Set<GroupObjectInstance> gridGroups) {return false;}
    public boolean dataUpdated(Collection<Property> changedProps) {return false;}
    public void fillProperties(Set<Property> properties) {}
    public boolean isInInterface(GroupObjectInstance classGroup) {return true;}

    public abstract Where order(Expr expr, boolean desc, Where orderWhere);

    public abstract ObjectValue refresh(SessionChanges session) throws SQLException;

    public abstract boolean isNull();
}
