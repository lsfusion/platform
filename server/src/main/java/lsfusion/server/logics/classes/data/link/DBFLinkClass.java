package lsfusion.server.logics.classes.data.link;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;

import java.util.ArrayList;
import java.util.Collection;

public class DBFLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "DBFLINK";
    }

    private static Collection<DBFLinkClass> instances = new ArrayList<>();

    public static DBFLinkClass get(boolean multiple) {
        for (DBFLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        DBFLinkClass instance = new DBFLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private DBFLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeID() {
        return DataType.DBFLINK;
    }

    @Override
    public String getDefaultCastExtension() {
        return "dbf";
    }
}