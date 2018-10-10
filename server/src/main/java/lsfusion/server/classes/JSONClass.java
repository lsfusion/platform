package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

import java.util.ArrayList;
import java.util.Collection;

public class JSONClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "JSONFILE";
    }

    private static Collection<JSONClass> instances = new ArrayList<>();

    public static JSONClass get(boolean multiple, boolean storeName) {
        for (JSONClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        JSONClass instance = new JSONClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private JSONClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return Data.JSON;
    }

    public String getOpenExtension(byte[] file) {
        return "json";
    }

    @Override
    public String getDefaultCastExtension() {
        return "json";
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        if (typeFrom instanceof StringClass) {
            return "cast_string_to_file(" + value + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }
}