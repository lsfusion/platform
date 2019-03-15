package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.property.DataType;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;

import java.util.ArrayList;
import java.util.Collection;

public class HTMLClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "HTMLFILE";
    }

    private static Collection<HTMLClass> instances = new ArrayList<>();

    public static HTMLClass get() {
        return get(false, false);
    }
    public static HTMLClass get(boolean multiple, boolean storeName) {
        for (HTMLClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        HTMLClass instance = new HTMLClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private HTMLClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.HTML;
    }

    public String getOpenExtension(RawFileData file) {
        return "html";
    }

    @Override
    public String getExtension() {
        return "html";
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        if (typeFrom instanceof StringClass) {
            return "cast_string_to_file(" + value + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        throw new UnsupportedOperationException();
    }
}