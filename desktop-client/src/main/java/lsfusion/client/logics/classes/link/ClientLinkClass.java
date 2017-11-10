package lsfusion.client.logics.classes.link;

import lsfusion.client.logics.classes.ClientDataClass;
import lsfusion.client.logics.classes.ClientTypeClass;
import lsfusion.interop.Compare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

import static lsfusion.interop.Compare.EQUALS;
import static lsfusion.interop.Compare.NOT_EQUALS;

public abstract class ClientLinkClass extends ClientDataClass implements ClientTypeClass {

    public final boolean multiple;

    protected ClientLinkClass(boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(multiple);
    }

    public String getPreferredMask() {
        return "1234567";
    }

    public Format getDefaultFormat() {
        return null;
    }

    public Object parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    @Override
    public Compare[] getFilterCompares() {
        return new Compare[] {EQUALS, NOT_EQUALS};
    }

    @Override
    public Compare getDefaultCompare() {
        return EQUALS;
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return obj == null ? "" : obj.toString();
    }
}