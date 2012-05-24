package platform.server.classes;

import platform.interop.Data;
import platform.server.caches.IdentityLazy;
import platform.server.data.sql.SQLSyntax;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class CustomStaticFormatFileClass extends StaticFormatFileClass {

    private String filterDescription;
    private String[] filterExtensions;

    private static String formatSID(boolean multiple, String filterDescription, String[] filterExtensions) {
        return "FileActionClass[multiple=" + multiple + ",  filterDescription=" + filterDescription + "," + Arrays.toString(filterExtensions) + "]";
    }

    protected String getFileSID() {
        throw new RuntimeException("SID overrided");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass.equals(this) ? this : null;
    }

    public CustomStaticFormatFileClass(boolean multiple, String filterDescription, String[] filterExtensions) {
        super(multiple);
        this.filterDescription = filterDescription;
        this.filterExtensions = filterExtensions;
    }

    public CustomStaticFormatFileClass(DataInputStream inStream) throws IOException {
        super(inStream);

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

    private static HashMap<String, CustomStaticFormatFileClass> instances = new HashMap<String, CustomStaticFormatFileClass>();

    public static CustomStaticFormatFileClass getDefinedInstance(boolean multiple, String description, String extensions) {
        String[] fextensions = extensions.split(" ");
        String sid = formatSID(multiple, description, fextensions);
        CustomStaticFormatFileClass instance = instances.get(sid);
        if (instance == null) {
            instance = new CustomStaticFormatFileClass(multiple, description, fextensions);
            instances.put(sid, instance);
            DataClass.storeClass(instance);
        }
        return instance;
    }

    @Override
    @IdentityLazy
    public String getSID() {
        return formatSID(multiple, filterDescription, filterExtensions);
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
