package lsfusion.server.classes;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.Data;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomStaticFormatFileClass extends StaticFormatFileClass {

    private String filterDescription;
    private ImSet<String> filterExtensions;

    protected String getFileSID() {
        if(filterExtensions.isEmpty())
            return "RAWFILE";
        return "CUSTOMFILE";
    }

    public static CustomStaticFormatFileClass get(boolean multiple, boolean storeName, String description, String extensions) {
        return get(multiple, storeName, description, SetFact.toExclSet(extensions.split(" ")));
    }

    private static List<CustomStaticFormatFileClass> instances = new ArrayList<>();
    
    public static CustomStaticFormatFileClass get() { // RAWFILE
        return get(false, false);
    }
    public static CustomStaticFormatFileClass get(boolean multiple, boolean storeName) {
        return get(multiple, storeName, "", SetFact.singleton(""));
    }
    public static CustomStaticFormatFileClass get(boolean multiple, boolean storeName, String description, ImSet<String> extensions) {
        if(extensions.contains("")) // если есть RAWFILE то и результат считаем RAWFILE
            extensions = SetFact.singleton("");
        
        for(CustomStaticFormatFileClass instance : instances)
            if(instance.multiple == multiple && instance.filterDescription.equals(description) && instance.filterExtensions.equals(extensions))
                return instance;

        CustomStaticFormatFileClass instance = new CustomStaticFormatFileClass(multiple, storeName, description, extensions);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private CustomStaticFormatFileClass(boolean multiple, boolean storeName, String filterDescription, ImSet<String> filterExtensions) {
        super(multiple, storeName);
        this.filterDescription = filterDescription;
        this.filterExtensions = filterExtensions;
    }

    @Override
    public String getOpenExtension(byte[] file) {
        return filterExtensions.get(0);
    }

    @Override
    protected ImSet<String> getExtensions() {
        return filterExtensions;
    }

    @Override
    public String getSID() {
        return super.getSID() + "_filterDescription=" + filterDescription + "_" + Arrays.toString(filterExtensions.toArray(new String[filterExtensions.size()])) + "]";
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeUTF(filterDescription);
        outStream.writeInt(filterExtensions.size());
        for (String extension : filterExtensions) {
            outStream.writeUTF(extension);
        }
    }

    @Override
    public byte getTypeID() {
        return Data.CUSTOMSTATICFORMATFILE;
    }
}
