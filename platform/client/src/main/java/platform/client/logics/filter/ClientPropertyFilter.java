package platform.client.logics.filter;

import platform.client.logics.ClientPropertyView;
import platform.client.logics.ClientValueLink;
import platform.interop.FilterType;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPropertyFilter {

    public ClientPropertyView property;
    public ClientValueLink value;

    public int compare;

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(FilterType.COMPARE);

        outStream.writeInt(property.ID);
        outStream.writeInt(compare);
        value.serialize(outStream);
    }    
}
