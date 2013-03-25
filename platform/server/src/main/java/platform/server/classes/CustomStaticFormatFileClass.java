package platform.server.classes;

import platform.interop.Data;

import java.io.DataInputStream;
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

    public DataClass getCompatible(DataClass compClass) {
        return compClass.equals(this) ? this : null;
    }

    public CustomStaticFormatFileClass(boolean multiple, boolean storeName, String filterDescription, String[] filterExtensions) {
        super(multiple, storeName);
        this.filterDescription = filterDescription;
        this.filterExtensions = filterExtensions;
    }

    public CustomStaticFormatFileClass(DataInputStream inStream, int version) throws IOException {
        super(inStream, version);

        filterDescription = inStream.readUTF();
        int extCount = inStream.readInt();
        if (extCount <= 0) {
            filterExtensions = new String[1];
            filterExtensions[0] = "*";
        } else {
            filterExtensions = new String[extCount];

            for (int i = 0; i < extCount; ++i) {
                filterExtensions[i] = inStream.readUTF();
            }
        }
    }

    @Override
    public String getOpenExtension() {
        return filterExtensions[0];
    }

    private static List<CustomStaticFormatFileClass> instances = new ArrayList<CustomStaticFormatFileClass>();

    public static CustomStaticFormatFileClass getDefinedInstance(boolean multiple, String description, String extensions){
        return getDefinedInstance(multiple, false, description, extensions);
    }

    public static CustomStaticFormatFileClass getDefinedInstance(boolean multiple, boolean storeName, String description, String extensions) {
        String[] fextensions = extensions.split(" ");
        
        for(CustomStaticFormatFileClass instance : instances)
            if(instance.multiple==multiple && instance.filterDescription.equals(description) && Arrays.equals(instance.filterExtensions, fextensions))
                return instance;

        CustomStaticFormatFileClass instance = new CustomStaticFormatFileClass(multiple, storeName, description, fextensions);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
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
