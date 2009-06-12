package platform.server.data.types;

public interface Reader<T> {
    T read(Object value);
}
