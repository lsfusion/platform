package platform.server.classes;

import platform.interop.Data;

public class WordClass extends FileClass {

    public final static WordClass instance = new WordClass();
    private final static String sid = "WordClass";

    public WordClass() {
        DataClass.storeClass(sid, instance);
    }

    public String toString() {
        return "Файл Word";
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof WordClass ? this : null;
    }

    public byte getTypeID() {
        return Data.WORD;
    }

    public String getSID() {
        return sid;
    }
}
