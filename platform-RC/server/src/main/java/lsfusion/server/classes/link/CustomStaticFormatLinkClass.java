package lsfusion.server.classes.link;

import lsfusion.interop.Data;
import lsfusion.server.classes.DataClass;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomStaticFormatLinkClass extends StaticFormatLinkClass {

    private String filterDescription;
    private String[] filterExtensions;

    protected String getFileSID() {
        return "CUSTOMLINK";
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass.equals(this) ? this : null;
    }

    public static CustomStaticFormatLinkClass get(boolean multiple, String description, String extensions) {
        return get(multiple, description, extensions.split(" "));
    }

    private static List<CustomStaticFormatLinkClass> instances = new ArrayList<>();

    public static CustomStaticFormatLinkClass get(boolean multiple, String description, String[] extensions) {
        for(CustomStaticFormatLinkClass instance : instances)
            if(instance.multiple == multiple && instance.filterDescription.equals(description) && Arrays.equals(instance.filterExtensions, extensions))
                return instance;

        CustomStaticFormatLinkClass instance = new CustomStaticFormatLinkClass(multiple, description, extensions);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private CustomStaticFormatLinkClass(boolean multiple, String filterDescription, String[] filterExtensions) {
        super(multiple);
        this.filterDescription = filterDescription;
        this.filterExtensions = filterExtensions;
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