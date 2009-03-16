package platform.client.logics;

import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientChangeCoeffValue extends ClientChangeValue {
    private Integer coeff;

    public ClientChangeCoeffValue(DataInputStream inStream) throws IOException {
        super(inStream);
        coeff = inStream.readInt();
    }

    public ClientObjectValue getObjectValue(Object value) {

        Object newValue;

        if (coeff.equals(1))
            newValue = value;
        else
            newValue = BaseUtils.multiply(value, coeff);

        return new ClientObjectValue(cls, newValue);
    }
}
