package lsfusion.server.logics.classes.data;

import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;

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
    protected boolean markupHtml() {
        return true;
    }
}
