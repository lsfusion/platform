package platform.server.data.type;

public class NullReader implements Reader<Object> {

    public static NullReader instance = new NullReader();

    public Object read(Object value) {
        assert value==null;
        return null;
    }
}
