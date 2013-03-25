package platform.server.classes;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class StaticFormatFileClass extends FileClass {

    public abstract String getOpenExtension();

    protected StaticFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    protected StaticFormatFileClass(DataInputStream inStream, int version) throws IOException {
        super(inStream, version);
    }
}
