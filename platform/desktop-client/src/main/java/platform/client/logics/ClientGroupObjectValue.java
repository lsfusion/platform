package platform.client.logics;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static platform.base.BaseUtils.*;

public class ClientGroupObjectValue extends HashMap<ClientObject, Object> implements Serializable {
    public static final ClientGroupObjectValue EMPTY = new ClientGroupObjectValue() {
        @Override
        public void putAll(Map<? extends ClientObject, ? extends Object> m) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public Object put(ClientObject key, Object value) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException("not supported");
        }
    };

    public ClientGroupObjectValue(ClientObject object, Object value) {
        put(object, value);
    }
    
    public ClientGroupObjectValue(ClientGroupObjectValue... clones) {
        super();
        for (ClientGroupObjectValue clone : clones) {
            putAll(clone);
        }
    }

    public ClientGroupObjectValue(DataInputStream inStream, ClientForm clientForm) throws IOException {
        int cnt = inStream.readInt();
        for (int i = 0; i < cnt; ++i) {
            int objId = inStream.readInt();
            put(clientForm.getObject(objId), deserializeObject(inStream));
        }
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(size());
        for (Map.Entry<ClientObject, Object> objectValue : entrySet()) {
            outStream.writeInt(objectValue.getKey().getID());
            serializeObject(outStream, objectValue.getValue());
        }
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        serialize(new DataOutputStream(outStream));
        return outStream.toByteArray();
    }

    public boolean contains(ClientGroupObjectValue other) {
        for (Map.Entry<ClientObject, Object> entry : other.entrySet()) {
            Object key = entry.getKey();
            if (!this.containsKey(key)) {
                return false;
            }

            if (!nullEquals(entry.getValue(), this.get(key))) {
                return false;
            }
        }

        return true;
    }

    public boolean contentEquals(ClientGroupObjectValue other) {
        return other != null && other.size() == size() && contains(other);
    }

    public void removeAll(Collection<ClientObject> keys) {
        for(ClientObject key : keys)
            remove(key);
    }
}
