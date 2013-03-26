package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.io.DataInputStream;
import java.io.IOException;

public class WordClass extends StaticFormatFileClass {

    public final static WordClass instance = new WordClass(false);
    public final static WordClass multipleInstance = new WordClass(true);

    protected String getFileSID() {
        return "WordClass";
    }

    static {
        DataClass.storeClass(instance, multipleInstance);
    }

    protected WordClass(boolean multiple) {
        super(multiple, false);
    }

    public WordClass(DataInputStream inStream, int version) throws IOException {
        super(inStream,version);
    }

    public String toString() {
        return ServerResourceBundle.getString("classes.word.file");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof WordClass ? this : null;
    }

    public byte getTypeID() {
        return Data.WORD;
    }

    public String getOpenExtension() {
        return "doc"; //,docx";
    }
}
