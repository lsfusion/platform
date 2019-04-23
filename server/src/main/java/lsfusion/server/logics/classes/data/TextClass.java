package lsfusion.server.logics.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;

public class TextClass extends StringClass {

    public final static StringClass instance = new TextClass(false);
    public final static StringClass richInstance = new TextClass(true);

    public final boolean rich;

    public TextClass(boolean rich) {
        super(LocalizedString.create("{classes.text}" + (rich ? " (rich)" : "")), false, ExtInt.UNLIMITED, false);

        this.rich = rich;
    }

    @Override
    public byte getTypeID() {
        return DataType.TEXT;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(rich);
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        if(compClass instanceof StringClass)
            return BaseUtils.cmp(rich, compClass instanceof TextClass && ((TextClass) compClass).rich, or) ? richInstance : instance;
        return super.getCompatible(compClass, or);
    }

    @Override
    public String getSID() {
        return rich ? "RICHTEXT" : "TEXT";
    }
}
