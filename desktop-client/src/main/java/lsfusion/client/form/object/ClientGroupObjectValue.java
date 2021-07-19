package lsfusion.client.form.object;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.client.form.ClientForm;

import java.io.*;
import java.util.*;

import static lsfusion.base.BaseUtils.*;

public class ClientGroupObjectValue implements Serializable {

    // we could use some other specific type (ClientObjectValue), but in that case we should "wrap" base types, and it we'll give some overhead
    private final Map<ClientObject, Serializable> map;

    private ClientGroupObjectValue() {
        map = Collections.emptyMap();
    }

    public static final ClientGroupObjectValue EMPTY = new ClientGroupObjectValue();

    public static final List<ClientGroupObjectValue> SINGLE_EMPTY_KEY_LIST = Collections.singletonList(EMPTY);

    public ClientGroupObjectValue(ClientObject object, Serializable value) {
        map = Collections.singletonMap(object, value);
    }
    
    public ClientGroupObjectValue(ClientGroupObjectValue... clones) {
        map = new HashMap<>();
        for (ClientGroupObjectValue clone : clones) {
            map.putAll(clone.map);
        }
    }

    public ClientGroupObjectValue(ClientGroupObjectValue clone, Collection<ClientObject> remove, ClientGroupObjectValue put) {
        map = new HashMap<>();
        map.putAll(clone.map);
        for(ClientObject key : remove)
            map.remove(key);
        map.putAll(put.map);
    }

    public ClientGroupObjectValue(DataInputStream inStream, ClientForm clientForm) throws IOException {
        map = new HashMap<>();

        int cnt = inStream.readInt();
        for (int i = 0; i < cnt; ++i) {
            int objId = inStream.readInt();
            map.put(clientForm.getObject(objId), deserializeObjectValue(inStream));
        }
    }

    public Iterable<Map.Entry<ClientObject, Serializable>> iterate() {
        return map.entrySet();
    }

    // should match FormChanges.deserializeObjectValue
    public static void serializeObjectValue(DataOutputStream outStream, Serializable value) throws IOException {
        if(value instanceof ClientCustomObjectValue) {
            outStream.writeByte(87);
            ClientCustomObjectValue objectValue = (ClientCustomObjectValue) value;
            outStream.writeLong(objectValue.id);
            Long idClass = objectValue.idClass;
            outStream.writeBoolean(idClass != null);
            if(idClass != null)
                outStream.writeLong(idClass);
            return;
        }

        BaseUtils.serializeObject(outStream, value);
    }

    // should match FormChanges.deserializeObjectValue
    private static Serializable deserializeObjectValue(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();
        if(type == 87) {
            long id = inStream.readLong();
            Long idClass = null;
            if(inStream.readBoolean())
                idClass = inStream.readLong();
            return new ClientCustomObjectValue(id, idClass);
        }

        return (Serializable) BaseUtils.deserializeObject(inStream, type);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(map.size());
        for (Map.Entry<ClientObject, Serializable> objectValue : map.entrySet()) {
            outStream.writeInt(objectValue.getKey().getID());
            serializeObjectValue(outStream, objectValue.getValue());
        }
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Serializable singleValue() {
        return BaseUtils.singleValue(map);
    }

    public byte[] serialize() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            serialize(new DataOutputStream(outStream));
            return outStream.toByteArray();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ClientGroupObjectValue && map.equals(((ClientGroupObjectValue) o).map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
