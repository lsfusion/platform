package lsfusion.client.classes.data;

import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.Compare;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;

import static lsfusion.interop.form.property.Compare.EQUALS;
import static lsfusion.interop.form.property.Compare.NOT_EQUALS;

public abstract class ClientFileClass extends ClientDataClass implements ClientTypeClass {

    public final boolean multiple;
    public final boolean storeName;

    protected ClientFileClass(boolean multiple, boolean storeName) {
        this.multiple = multiple;
        this.storeName = storeName;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(multiple);
        outStream.writeBoolean(storeName);
    }

    @Override
    public int getDefaultWidth(FontMetrics fontMetrics, ClientPropertyDraw property) {
        return 18;
    }

    public Object parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    @Override
    public Compare[] getFilterCompares() {
        return new Compare[] {EQUALS, NOT_EQUALS};
    }
}