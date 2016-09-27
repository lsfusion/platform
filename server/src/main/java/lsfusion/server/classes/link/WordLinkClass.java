package lsfusion.server.classes.link;

import lsfusion.interop.Data;
import lsfusion.server.classes.DataClass;
import lsfusion.server.logics.ServerResourceBundle;

import java.util.ArrayList;
import java.util.Collection;

public class WordLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "WORDLINK";
    }

    private static Collection<WordLinkClass> instances = new ArrayList<>();

    public static WordLinkClass get(boolean multiple) {
        for (WordLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        WordLinkClass instance = new WordLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private WordLinkClass(boolean multiple) {
        super(multiple);
    }

    public String toString() {
        return ServerResourceBundle.getString("classes.word.link");
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof WordLinkClass ? this : null;
    }

    public byte getTypeID() {
        return Data.WORDLINK;
    }
}