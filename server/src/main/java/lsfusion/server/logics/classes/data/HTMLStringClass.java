package lsfusion.server.logics.classes.data;

import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.file.FileClass;

import java.io.DataOutputStream;
import java.io.IOException;

public class HTMLStringClass extends StringClass {

    public HTMLStringClass() {
        super(false, ExtInt.UNLIMITED, false);
    }

    public final static HTMLStringClass instance = new HTMLStringClass();

    @Override
    public byte getTypeID() {
        return DataType.HTMLSTRING;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());
    }

    @Override
    public String getSID() {
        return "HTML";
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof HTMLStringClass ? this : super.getCompatible(compClass, or);
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if(typeFrom instanceof FileClass)
            return ((FileClass) typeFrom).getCastToConvert(true, value, syntax);

        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
    }

    @Override
    public boolean useInputTag(boolean isPanel, boolean useBootstrap, Type changeType) {
        return false;
    }

    @Override
    public boolean markupHtml() {
        return true;
    }
}
