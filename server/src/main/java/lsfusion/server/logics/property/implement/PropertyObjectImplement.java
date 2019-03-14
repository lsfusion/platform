package lsfusion.server.logics.property.implement;

public class PropertyObjectImplement<T> implements PropertyObjectInterfaceImplement<T> {

    public final T object;

    public PropertyObjectImplement(T object) {
        this.object = object;
    }
}
