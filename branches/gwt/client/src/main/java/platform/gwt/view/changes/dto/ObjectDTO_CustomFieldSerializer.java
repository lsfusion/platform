package platform.gwt.view.changes.dto;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

public class ObjectDTO_CustomFieldSerializer {
    public static void deserialize(SerializationStreamReader in, ObjectDTO instance) throws SerializationException {
        // Handled in instantiate.
    }

    public static ObjectDTO instantiate(SerializationStreamReader in) throws SerializationException {
        return new ObjectDTO(in.readObject());
    }

    public static void serialize(SerializationStreamWriter out, ObjectDTO instance) throws SerializationException {
        out.writeObject(instance.getValue());
    }
}
