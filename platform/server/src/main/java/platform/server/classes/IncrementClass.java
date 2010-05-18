package platform.server.classes;

public abstract class IncrementClass<T> extends DataClass<T> {

    public Object shift(Object object) {
        return shiftValue(read(object));
    }

    public abstract T shiftValue(T object);
}
