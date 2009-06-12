package platform.server.session;

import platform.server.data.classes.ConcreteValueClass;

import java.io.DataOutputStream;
import java.io.IOException;

public class ChangeCoeffValue extends ChangeValue {
    public Integer coeff;

    public ChangeCoeffValue(ConcreteValueClass iClass, Integer iCoeff) {
        super(iClass);
        coeff = iCoeff;
    }

    byte getTypeID() {
        return 1;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(coeff);
    }
}
