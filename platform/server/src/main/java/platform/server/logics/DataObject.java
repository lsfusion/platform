package platform.server.logics;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.classes.ConcreteClass;
import platform.server.classes.LogicalClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.view.form.PropertyObjectInterface;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.navigator.PropertyInterfaceNavigator;
import platform.server.view.navigator.Mapper;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.caches.hash.HashValues;
import platform.server.session.ChangesSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.sql.SQLException;

public class DataObject extends ObjectValue<DataObject> implements PropertyObjectInterface, PropertyInterfaceNavigator {

    public Object object;
    public ConcreteClass objectClass;

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof DataObject && object.equals(((DataObject)o).object) && objectClass.equals(((DataObject)o).objectClass);
    }

    public DataObject(Object object, ConcreteClass objectClass) {
        this.object = object;

        assert BaseUtils.isData(object);
        assert !(objectClass instanceof LogicalClass && !object.equals(true));

        this.objectClass = objectClass;
    }

    public boolean isString(SQLSyntax syntax) {
        return objectClass.getType().isSafeString(object);
    }
    public String getString(SQLSyntax syntax) {
        return objectClass.getType().getString(object, syntax);
    }

    public ValueExpr getExpr() {
        return new ValueExpr(object,objectClass);
    }

    public Object getValue() {
        return object;
    }

    public static <K> Map<K,ValueExpr> getMapValueExprs(Map<K,DataObject> map) {
        Map<K,ValueExpr> mapExprs = new HashMap<K,ValueExpr>();
        for(Map.Entry<K,DataObject> keyField : map.entrySet())
            mapExprs.put(keyField.getKey(), keyField.getValue().getExpr());
        return mapExprs;
    }

    public static <K> Map<K,Object> getMapValues(Map<K,DataObject> map) {
        Map<K,Object> mapClasses = new HashMap<K,Object>();
        for(Map.Entry<K,DataObject> keyField : map.entrySet())
            mapClasses.put(keyField.getKey(), keyField.getValue().object);
        return mapClasses;
    }

    public static <K> Map<K,ConcreteClass> getMapClasses(Map<K,DataObject> map) {
        Map<K,ConcreteClass> mapClasses = new HashMap<K,ConcreteClass>();
        for(Map.Entry<K,DataObject> keyField : map.entrySet())
            mapClasses.put(keyField.getKey(), keyField.getValue().objectClass);
        return mapClasses;        
    }

    public Where order(Expr expr, boolean desc, Where orderWhere) {
        Where greater = expr.compare(this,Compare.GREATER);
        return (desc?greater.not():greater).or(expr.compare(this,Compare.EQUALS).and(orderWhere));
    }

    public AndClassSet getClassSet(GroupObjectImplement classGroup) {
        return objectClass;
    }

    public DataObject getDataObject() {
        return this;
    }

    public ConcreteClass getCurrentClass() {
        return objectClass;
    }

    public GroupObjectImplement getApplyObject() {
        return null;
    }

    public Type getType() {
        return objectClass.getType();
    }

    public PropertyObjectInterface doMapping(Mapper mapper) {
        return this;
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
    }

    public int hashValues(HashValues hashValues) {
        return hashValues.hash(getExpr());
    }

    public Set<ValueExpr> getValues() {
        return Collections.singleton(getExpr());
    }

    private DataObject(ValueExpr expr) {
        this(expr.object,expr.objectClass);
    }

    public DataObject translate(Map<ValueExpr, ValueExpr> mapValues) {
        return new DataObject(mapValues.get(getExpr()));
    }

    public DataObject refresh(ChangesSession session) throws SQLException {
        return session.getDataObject(object, objectClass.getType());
    }
}
