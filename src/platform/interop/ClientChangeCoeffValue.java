package platform.interop;

import platform.base.BaseUtils;

public class ClientChangeCoeffValue extends ClientChangeValue {
    Integer coeff;

    public ClientChangeCoeffValue(ClientClass icls, Integer icoeff) {
        super(icls);
        coeff = icoeff;
    }

    public ClientObjectValue getObjectValue(Object value) {

        Object newValue = value;

        if (coeff.equals(1))
            newValue = value;
        else
            newValue = BaseUtils.multiply(value, coeff);

        return new ClientObjectValue(cls, newValue);
    }
}
