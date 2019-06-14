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

public class TXTClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "TEXTFILE";
    }

    private static Collection<TXTClass> instances = new ArrayList<>();

    public static TXTClass get() {
        return get(false, false);
    }

    public static TXTClass get(boolean multiple, boolean storeName) {
        for (TXTClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        TXTClass instance = new TXTClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private TXTClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.TXT;
    }

    public String getOpenExtension(RawFileData file) {
        return "txt";
    }

    @Override
    public String getExtension() {
        return "txt";
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        throw new UnsupportedOperationException();
    }
}