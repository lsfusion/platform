package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class WordClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "WordClass";
    }

    private static Collection<WordClass> instances = new ArrayList<WordClass>();

    public static WordClass get(boolean multiple, boolean storeName) {
        for (WordClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        WordClass instance = new WordClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private WordClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
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

    public String getOpenExtension(byte[] file) {
        return file.length<=2 ? "doc" : ((file[0] == 80 && file[1] == 75) ? "docx" : "doc");

    }
}
