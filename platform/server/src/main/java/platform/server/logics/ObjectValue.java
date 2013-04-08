package platform.server.logics;

import platform.base.FunctionSet;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.AbstractValuesContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.filter.CompareValue;
import platform.server.logics.property.CalcProperty;
import platform.server.session.Modifier;
import platform.server.session.SessionChanges;

import java.sql.SQLException;
import java.util.Collection;

public abstract class ObjectValue<T extends ObjectValue<T>> extends AbstractValuesContext<T> implements CompareValue {

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

    public Expr getExpr(ImMap<ObjectInstance, ? extends Expr> classSource, Modifier modifier) {
        return getExpr();
    }

    public static <K> ImMap<K,Expr> getMapExprs(ImMap<K,? extends ObjectValue> map) {
        return ((ImMap<K, ObjectValue>)map).mapValues(new GetValue<Expr, ObjectValue>() {
            public Expr getMapValue(ObjectValue value) {
                return value.getExpr();
            }});
    }

    public static <K> ImMap<K,ConcreteClass> getMapClasses(ImMap<K,ObjectValue> map) {
        return DataObject.getMapClasses(DataObject.filterDataObjects(map));
    }

    public static <K> boolean containsNull(Collection<ObjectValue> col) {
        for(ObjectValue value : col)
            if(value instanceof NullValue)
                return true;
        return false;
    }

    
    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {return false;}
    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {return false;}
    public boolean dataUpdated(FunctionSet<CalcProperty> changedProps) {return false;}
    public void fillProperties(MSet<CalcProperty> properties) {}
    public boolean isInInterface(GroupObjectInstance classGroup) {return true;}

    public abstract Where order(Expr expr, boolean desc, Where orderWhere);

    public abstract ObjectValue refresh(SessionChanges session, ValueClass upClass) throws SQLException;

    public abstract boolean isNull();

    public abstract <K> ClassWhere<K> getClassWhere(K key);
}
