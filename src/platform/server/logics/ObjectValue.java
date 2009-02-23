package platform.server.logics;

import platform.server.logics.classes.DataClass;

public class ObjectValue {
    public Integer object;
    public DataClass objectClass;

    public boolean equals(Object o) {
        return this==o || o instanceof ObjectValue && object.equals(((ObjectValue) o).object);
    }

    public int hashCode() {
        return object.hashCode();
    }

    public ObjectValue(Integer iObject, DataClass iClass) {
        object =iObject;
        objectClass =iClass;}
}
