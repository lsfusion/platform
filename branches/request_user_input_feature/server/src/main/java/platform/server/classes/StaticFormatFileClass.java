package platform.server.classes;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class StaticFormatFileClass extends FileClass {

    public abstract String getOpenExtension();

    protected StaticFormatFileClass(boolean multiple) {
        super(multiple);
    }

    protected StaticFormatFileClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }
}
