package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

import java.util.ArrayList;
import java.util.Collection;

public class DynamicFormatFileClass extends FileClass {

    protected String getFileSID() {
        return "CUSTOMFILE"; // для обратной совместимости такое название
    }

    private static Collection<DynamicFormatFileClass> instances = new ArrayList<>();

    public static DynamicFormatFileClass get(boolean multiple, boolean storeName) {
        for (DynamicFormatFileClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        DynamicFormatFileClass instance = new DynamicFormatFileClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private DynamicFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DynamicFormatFileClass ? this : null;
    }

    public byte getTypeID() {
        return Data.DYNAMICFORMATFILE;
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        String extension = null;
        if (typeFrom instanceof StaticFormatFileClass) {
            extension = ((StaticFormatFileClass) typeFrom).getDefaultCastExtension();
        }
        if (extension != null) {
            return "casttocustomfile(" + value + ", CAST('" + extension + "' AS VARCHAR))";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }

    @Override
    protected byte[] parseHTTPNotNull(byte[] b) {
        return b;
    }

    @Override
    protected byte[] formatHTTPNotNull(byte[] b) {
        return b;
    }
}
