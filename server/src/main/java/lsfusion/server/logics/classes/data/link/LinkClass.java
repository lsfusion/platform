package lsfusion.server.logics.classes.data.link;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.file.FileClass;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class LinkClass extends StringClass {

    public final boolean multiple;

    protected LinkClass(boolean multiple) {
        super(false, ExtInt.UNLIMITED, false);

        this.multiple = multiple;
    }

    @Override
    public String getValueAlignmentHorz() {
        return "center";
    }

    protected abstract String getFileSID();

    @Override
    public String getSID() {
        return getFileSID() + (multiple ? "_Multiple" : "");
    }

    @Override
    public String getCanonicalName() {
        return getFileSID();
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());
        outStream.writeBoolean(multiple);
    }


    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if(typeFrom instanceof FileClass)
            return ((FileClass) typeFrom).getCastToConvert(false, value, syntax);

        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
    }
}