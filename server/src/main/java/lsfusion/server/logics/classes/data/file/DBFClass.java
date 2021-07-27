package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

import java.util.ArrayList;
import java.util.Collection;

public class DBFClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "DBFFILE";
    }

    private static Collection<DBFClass> instances = new ArrayList<>();

    public static DBFClass get() {
        return get(false, false);
    }
    public static DBFClass get(boolean multiple, boolean storeName) {
        for (DBFClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        DBFClass instance = new DBFClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private DBFClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.DBF;
    }

    public String getOpenExtension(RawFileData file) {
        return "dbf";
    }

    @Override
    public String getExtension() {
        return "dbf";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        throw new UnsupportedOperationException();
    }
}
