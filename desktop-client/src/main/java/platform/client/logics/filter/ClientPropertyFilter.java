package platform.client.logics.filter;

import platform.client.logics.ClientFilterValue;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Compare;
import platform.interop.FilterType;

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
