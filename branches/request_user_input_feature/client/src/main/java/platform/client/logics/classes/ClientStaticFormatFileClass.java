package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class ClientStaticFormatFileClass extends ClientFileClass {

    protected ClientStaticFormatFileClass() {
    }

    protected ClientStaticFormatFileClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }
}
