package platform.server.logics.property.linear;

import platform.server.logics.property.MultiplyFormulaProperty;
import platform.server.logics.property.StringFormulaPropertyInterface;

public class LMFP extends LP<StringFormulaPropertyInterface, MultiplyFormulaProperty> {

    public LMFP(MultiplyFormulaProperty iProperty) {
        super(iProperty);
        listInterfaces.addAll(property.interfaces);
    }
}
