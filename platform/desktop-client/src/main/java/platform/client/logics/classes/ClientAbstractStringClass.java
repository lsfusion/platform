package platform.client.logics.classes;

import platform.interop.Compare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

import static platform.interop.Compare.CONTAINS;

public abstract class ClientAbstractStringClass extends ClientDataClass {

    public final boolean caseInsensitive;

    public ClientAbstractStringClass(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(caseInsensitive);
    }

    public Format getDefaultFormat() {
        return null;
    }

    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public String formatString(Object obj) {
        return obj.toString();
    }

    @Override
    public Object transformServerValue(Object obj) {
        return obj == null ? null : formatString(obj);
    }

    @Override
    public Compare[] getFilterCompares() {
        return Compare.values();
    }

    @Override
    public Compare getDefaultCompare() {
        return CONTAINS;
    }
}
