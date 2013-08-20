package lsfusion.server.classes;

import lsfusion.interop.Data;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomStaticFormatFileClass extends StaticFormatFileClass {

    private String filterDescription;
    private String[] filterExtensions;

    protected String getFileSID() {
        return "CustomStaticFormatFileClass";
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass.equals(this) ? this : null;
    }

    public static CustomStaticFormatFileClass get(boolean multiple, boolean storeName, String description, String extensions) {
        return get(multiple, storeName, description, extensions.split(" "));
    }

    private static List<CustomStaticFormatFileClass> instances = new ArrayList<CustomStaticFormatFileClass>();

    public static CustomStaticFormatFileClass get(boolean multiple, boolean storeName, String description, String[] extensions) {
        for(CustomStaticFormatFileClass instance : instances)
            if(instance.multiple == multiple && instance.filterDescription.equals(description) && Arrays.equals(instance.filterExtensions, extensions))
                return instance;

        CustomStaticFormatFileClass instance = new CustomStaticFormatFileClass(multiple, storeName, description, extensions);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private CustomStaticFormatFileClass(boolean multiple, boolean storeName, String filterDescription, String[] filterExtensions) {
        super(multiple, storeName);
        this.filterDescription = filterDescription;
        this.filterExtensions = filterExtensions;
    }

    @Override
    public String getOpenExtension(byte[] file) {
        return filterExtensions[0];
    }

    @Override
    public String getSID() {
        return super.getSID() + "_filterDescription=" + filterDescription + "_" + Arrays.toString(filterExtensions) + "]";
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeUTF(filterDescription);
        outStream.writeInt(filterExtensions.length);
        for (String extension : filterExtensions) {
            outStream.writeUTF(extension);
        }
    }

    @Override
    public byte getTypeID() {
        return Data.CUSTOMSTATICFORMATFILE;
    }
}
