package lsfusion.client.classes.data.link;

import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.classes.data.ClientDataClass;
import lsfusion.client.classes.data.ClientStringClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;

import static lsfusion.interop.form.property.Compare.EQUALS;

public abstract class ClientLinkClass extends ClientStringClass implements ClientTypeClass {

    public final boolean multiple;

    protected ClientLinkClass(boolean multiple) {
        super(false, false, ExtInt.UNLIMITED);

        this.multiple = multiple;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeClass().getTypeId());

        outStream.writeBoolean(multiple);
    }
}