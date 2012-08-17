package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.interop.Data;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientInsensitiveStringClass extends ClientStringClass {
    public ClientInsensitiveStringClass(DataInputStream inStream) throws IOException {
        super(inStream);

        sID = "InsensitiveStringClass[" + length + "]";
    }

    public ClientInsensitiveStringClass(int length) {
        super(length);
    }

    public final static ClientTypeClass type = new ClientTypeClass() {
        public byte getTypeId() {
            return Data.INSENSITIVESTRING;
        }

        public ClientInsensitiveStringClass getDefaultClass(ClientObjectClass baseClass) {
            return getDefaultType();
        }

        public ClientInsensitiveStringClass getDefaultType() {
            return new ClientInsensitiveStringClass(50);
        }

        @Override
        public String toString() {
            return ClientResourceBundle.getString("logics.classes.unsensitive.string");
        }
    };
    public ClientTypeClass getTypeClass() {
        return type;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.unsensitive.string")+"(" + length + ")";
    }
}
