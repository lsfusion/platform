package platform.server.logics.session;

import platform.server.logics.classes.DataClass;

public class ChangeCoeffValue extends ChangeValue {
    public Integer Coeff;

    public ChangeCoeffValue(DataClass iClass, Integer iCoeff) {
        super(iClass);
        Coeff = iCoeff;
    }
}
