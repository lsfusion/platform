package platform.server.classes;

public abstract class IncrementClass<T> extends DataClass<T> {

    public Object shift(Object object, boolean back) {
        return shiftValue(read(object), back);
    }

    public abstract T shiftValue(T object, boolean back);
}
