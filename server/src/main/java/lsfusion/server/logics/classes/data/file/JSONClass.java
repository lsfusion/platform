package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

import java.util.ArrayList;
import java.util.Collection;

public class JSONClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "JSONFILE";
    }

    private static Collection<JSONClass> instances = new ArrayList<>();

    public static JSONClass get() {
        return get(false, false);
    }

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
        return DataType.JSON;
    }

    public String getOpenExtension(RawFileData file) {
        return "json";
    }

    @Override
    public String getExtension() {
        return "json";
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        return FormIntegrationType.JSON;
    }
}