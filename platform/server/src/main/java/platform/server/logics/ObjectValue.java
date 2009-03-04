package platform.server.logics;

import platform.server.logics.classes.RemoteClass;

public class ObjectValue {
    public Integer object;
    public RemoteClass objectClass;

    public boolean equals(Object o) {
        return this==o || o instanceof ObjectValue && object.equals(((ObjectValue) o).object);
    }

    public int hashCode() {
        return object.hashCode();
    }

    public ObjectValue(Integer iObject, RemoteClass iClass) {
        object =iObject;
        objectClass =iClass;}
}
