package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

import java.util.ArrayList;
import java.util.Collection;

public class CSVClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "CSVFILE";
    }

    private static Collection<CSVClass> instances = new ArrayList<>();

    public static CSVClass get(boolean multiple, boolean storeName) {
        for (CSVClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        CSVClass instance = new CSVClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private CSVClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return Data.CSV;
    }

    public String getOpenExtension(byte[] file) {
        return "csv";
    }

    @Override
    public String getDefaultCastExtension() {
        return "csv";
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        if (typeFrom instanceof StringClass) {
            return "cast_string_to_file(" + value + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }
}