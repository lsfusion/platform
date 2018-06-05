package lsfusion.server.classes;

import lsfusion.interop.Data;
import org.apache.poi.poifs.filesystem.DocumentFactoryHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class WordClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "WORDFILE";
    }

    private static Collection<WordClass> instances = new ArrayList<>();

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

    public byte getTypeID() {
        return Data.WORD;
    }

    public String getOpenExtension(byte[] file) {
        try {
            return DocumentFactoryHelper.hasOOXMLHeader(new ByteArrayInputStream(file)) ? "docx" : "doc";
        } catch (IOException e) {
            return "doc";
        }
    }

    @Override
    public String getDefaultCastExtension() {
        return "doc";
    }
}
