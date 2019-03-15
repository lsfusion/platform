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

public class XMLClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "XMLFILE";
    }

    private static Collection<XMLClass> instances = new ArrayList<>();

    public static XMLClass get() {
        return get(false, false);
    }
    
    public static XMLClass get(boolean multiple, boolean storeName) {
        for (XMLClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        XMLClass instance = new XMLClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private XMLClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.XML;
    }

    public String getOpenExtension(RawFileData file) {
        return "xml";
    }

    @Override
    public String getExtension() {
        return "xml";
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
        return FormIntegrationType.XML;
    }
}