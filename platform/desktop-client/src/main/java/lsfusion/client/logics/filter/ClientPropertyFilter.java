package lsfusion.client.logics.filter;

import lsfusion.client.logics.ClientFilterValue;
import lsfusion.client.logics.ClientGroupObject;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Compare;
import lsfusion.interop.FilterType;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPropertyFilter {

    public ClientGroupObject groupObject;
    public ClientPropertyDraw property;
    public ClientFilterValue value;

    public boolean negation;
    public Compare compare;
    public boolean junction = true; //true - conjunction, false - disjunction

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(FilterType.COMPARE);

        outStream.writeInt(property.getID());
        outStream.writeBoolean(negation);
        compare.serialize(outStream);
        value.serialize(outStream);
        outStream.writeBoolean(junction);
    }    
}
