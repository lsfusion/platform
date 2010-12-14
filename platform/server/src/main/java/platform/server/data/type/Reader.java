package platform.server.data.type;

public interface Reader<T> {
    T read(Object value);
}
