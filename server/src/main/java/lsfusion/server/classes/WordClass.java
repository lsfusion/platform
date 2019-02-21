package lsfusion.server.classes;

import lsfusion.base.RawFileData;
import lsfusion.interop.Data;
import lsfusion.server.logics.property.actions.integration.FormIntegrationType;
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

    public static WordClass get() {
        return get(false, false);
    }
    
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

    public String getOpenExtension(RawFileData file) {
        try {
            return DocumentFactoryHelper.hasOOXMLHeader(file.getInputStream()) ? "docx" : "doc";
        } catch (IOException e) {
            return "doc";
        }
    }

    @Override
    public String getExtension() {
        return "doc";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        throw new UnsupportedOperationException();
    }
}
