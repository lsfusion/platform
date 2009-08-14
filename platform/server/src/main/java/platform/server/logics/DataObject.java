package platform.server.logics;

import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.LogicalClass;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;
import platform.base.BaseUtils;

import java.util.HashMap;
import java.util.Map;

public class DataObject extends ObjectValue {

    public Object object;
    public ConcreteClass objectClass;

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof DataObject && object.equals(((DataObject)o).object) && objectClass.equals(((DataObject)o).objectClass);
    }

    public int hashCode() {
        return object.hashCode()*31+objectClass.hashCode();
    }

    public DataObject(Object iObject, ConcreteClass iClass) {
        object = iObject;

        assert BaseUtils.isData(object);
        assert !(objectClass instanceof LogicalClass && !object.equals(true));

        objectClass = iClass;
    }

    public boolean isString(SQLSyntax syntax) {
        return objectClass.getType().isSafeString(object);
    }
    public String getString(SQLSyntax syntax) {
        return objectClass.getType().getString(object, syntax);
    }

    public ValueExpr getExpr() {
        return new ValueExpr(this);
    }

    public Object getValue() {
        return object;
    }

    public static <K> Map<K,ConcreteClass> getMapClasses(Map<K,DataObject> map) {
        Map<K,ConcreteClass> mapClasses = new HashMap<K,ConcreteClass>();
        for(Map.Entry<K,DataObject> keyField : map.entrySet())
            mapClasses.put(keyField.getKey(), keyField.getValue().objectClass);
        return mapClasses;        
    }
}
