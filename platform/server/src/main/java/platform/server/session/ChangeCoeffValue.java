package platform.server.session;

import platform.server.logics.classes.RemoteClass;

import java.io.DataOutputStream;
import java.io.IOException;

public class ChangeCoeffValue extends ChangeValue {
    public Integer coeff;

    public ChangeCoeffValue(RemoteClass iClass, Integer iCoeff) {
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
