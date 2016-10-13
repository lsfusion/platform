package lsfusion.server.logics;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.AbstractValuesContext;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.Field;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.instance.*;
import lsfusion.server.form.instance.filter.CompareValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.SessionChanges;

import java.sql.SQLException;
import java.util.Collection;

public abstract class ObjectValue<T extends ObjectValue<T>> extends AbstractValuesContext<T> implements CompareValue {

    public abstract String getString(SQLSyntax syntax);

    public abstract boolean isSafeString(SQLSyntax syntax);

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

    public Expr getExpr(ImMap<ObjectInstance, ? extends Expr> classSource, Modifier modifier, ReallyChanged reallyChanged) {
        return getExpr();
    }

    public static <K> ImMap<K,Expr> getMapExprs(ImMap<K,? extends ObjectValue> map) {
        return ((ImMap<K, ObjectValue>)map).mapValues(new GetValue<Expr, ObjectValue>() {
            public Expr getMapValue(ObjectValue value) {
                return value.getExpr();
            }});
    }

    public static <K> ImMap<K,ConcreteClass> getMapClasses(ImMap<K,ObjectValue> map) {
        return DataObject.getMapDataClasses(DataObject.filterDataObjects(map));
    }

    public static <K> ImMap<K,Object> getMapValues(ImMap<K,ObjectValue> map) {
        return map.mapValues(new GetValue<Object, ObjectValue>() {
            public Object getMapValue(ObjectValue value) {
                return value.getValue();
            }});
    }

    public static <K> boolean containsNull(Collection<ObjectValue> col) {
        for(ObjectValue value : col)
            if(value instanceof NullValue)
                return true;
        return false;
    }

    
    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {return false;}
    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {return false;}
    public boolean dataUpdated(ChangedData changedProps, ReallyChanged reallyChanged, Modifier modifier, boolean hidden, ImSet<GroupObjectInstance> groupObjects) {return false;}
    public void fillProperties(MSet<CalcProperty> properties) {}
    public boolean isInInterface(GroupObjectInstance classGroup) {return true;}

    public abstract Where order(Expr expr, boolean desc, Where orderWhere);

    public abstract ObjectValue refresh(SessionChanges session, ValueClass upClass) throws SQLException, SQLHandledException;

    public abstract boolean isNull();

    public abstract <K> ClassWhere<K> getClassWhere(K key);
    
    public abstract ParseInterface getParse(Field field, SQLSyntax syntax);

    public abstract String getShortName();
}
