package lsfusion.client.logics.filter;

import lsfusion.client.form.filter.ClientFilterValue;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.Compare;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPropertyFilter {

    public ClientGroupObject groupObject;
    public ClientPropertyDraw property;
    public ClientFilterValue value;

    public ClientGroupObjectValue columnKey; // nullable означает что надо текущий брать

    public boolean negation;
    public Compare compare;
    public boolean junction = true; //true - conjunction, false - disjunction

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(property.getID());
        outStream.writeBoolean(columnKey != null);
        if(columnKey != null)
            columnKey.serialize(outStream);
        outStream.writeBoolean(negation);
        compare.serialize(outStream);
        value.serialize(outStream);
        outStream.writeBoolean(junction);
    }

    public Compare getDefaultCompare() {
        return property.defaultCompare != null ? property.defaultCompare : property.baseType.getDefaultCompare();
    }
}
