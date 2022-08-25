package lsfusion.client.classes.data.link;

import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.classes.data.ClientDataClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.Compare;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;

import static lsfusion.interop.form.property.Compare.EQUALS;

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

    @Override
    public int getDefaultWidth(FontMetrics fontMetrics, ClientPropertyDraw property) {
        return 18;
    }

    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public Compare[] getFilterCompares() {
        return new Compare[] {EQUALS};
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return obj == null ? "" : obj.toString();
    }
}